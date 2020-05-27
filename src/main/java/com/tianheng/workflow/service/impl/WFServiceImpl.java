package com.tianheng.workflow.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tianheng.workflow.entity.Assignment;
import com.tianheng.workflow.service.WFService;
import com.tianhengyun.common.tang4jbase.exception.ValidateException;
import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ModelQuery;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.image.ProcessDiagramGenerator;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class WFServiceImpl implements WFService {

    private Logger logger = LoggerFactory.getLogger(WFServiceImpl.class);

    @Autowired
    private ProcessEngine processEngine;
    @Autowired
    private ObjectMapper objectMapper;


    @Override
    public void createModel(HttpServletResponse response) throws IOException {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        //初始化一个空模型
        Model model = repositoryService.newModel();

        //设置一些默认信息
        String name = "new-process";
        String description = "";
        int revision = 1;
        String key = "process";

        ObjectNode modelNode = objectMapper.createObjectNode();
        modelNode.put(ModelDataJsonConstants.MODEL_NAME, name);
        modelNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION, description);
        modelNode.put(ModelDataJsonConstants.MODEL_REVISION, revision);

        model.setName(name);
        model.setKey(key);
        model.setMetaInfo(modelNode.toString());

        repositoryService.saveModel(model);
        String id = model.getId();

        //完善ModelEditorSource
        ObjectNode editorNode = objectMapper.createObjectNode();
        editorNode.put("id", "canvas");
        editorNode.put("resourceId", "canvas");
        ObjectNode stencilSetNode = objectMapper.createObjectNode();
        stencilSetNode.put("namespace",
                "http://b3mn.org/stencilset/bpmn2.0#");
        editorNode.put("stencilset", stencilSetNode);
        repositoryService.addModelEditorSource(id, editorNode.toString().getBytes("utf-8"));
        response.sendRedirect("/workflow/modeler.html?modelId=" + id);
    }

    @Override
    public boolean delModel(String modelId) {
        processEngine.getRepositoryService().deleteModel(modelId);
        return true;
    }

    @Override
    public List<Model> models() {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        return repositoryService.createModelQuery().list();
    }

    @Override
    public IPage<Model> pageModels(int page, int size) {
        ModelQuery modelQuery = processEngine.getRepositoryService().createModelQuery();
        long total = modelQuery.count();
        List<Model> models = modelQuery.desc().listPage(page, size);
        return new Page<Model>(page, size).setRecords(models).setTotal(total);
    }

    @Override
    public boolean publish(String modelId) throws IOException {

        if (StringUtils.isEmpty(modelId)) {
            throw new ValidateException("the modelId cannot be empty");
        }
        //获取模型
        RepositoryService repositoryService = processEngine.getRepositoryService();
        Model modelData = repositoryService.getModel(modelId);
        if (modelData != null) {
            if (StringUtils.isBlank(modelData.getDeploymentId())) {//deploymentId不为空则移除发布的流程，否则发布流程
                byte[] bytes = repositoryService.getModelEditorSource(modelData.getId());
                if (bytes == null) {
                    throw new ValidateException("the model data is empty，Please design the process first and save it successfully，then publish 。");
                }
                JsonNode modelNode = new ObjectMapper().readTree(bytes);

                BpmnModel model = new BpmnJsonConverter().convertToBpmnModel(modelNode);
                if (model.getProcesses().size() == 0) {
                    throw new ValidateException(" the model Data does not meet the requirements，Please design at least one main flow。");
                }
                byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(model);

                //如果流程节点未配置参与者不允许启动流程
                hasCompleteAssignment(modelId);
                //发布流程
                String processName = modelData.getName() + ".bpmn20.xml";
                Deployment deployment = repositoryService.createDeployment()
                        .name(modelData.getName())
                        .addString(processName, new String(bpmnBytes, "UTF-8"))
                        .deploy();
                modelData.setDeploymentId(deployment.getId());
                repositoryService.saveModel(modelData);
            } else {
                //普通删除，如果正当前规则下有正在执行的流程，则抛出异常
                //repositoryService.deleteDeployment(modelData.getDeploymentId());
                //级联删除，会删除当前规则相关的所有信息，正在执行的信息，包括历史信息
                repositoryService.deleteDeployment(modelData.getDeploymentId(), true);

            }
        } else {
            throw new ValidateException("the model does not exit。");
        }
        return true;
    }

    @Override
    public String startProcess(String deploymentId, String businessKey) throws IOException {

        if (!StringUtils.isBlank(deploymentId) && !StringUtils.isBlank(businessKey)) {
            RepositoryService repositoryService = processEngine.getRepositoryService();
            //获取发布模型
            Deployment deployment = processEngine.getRepositoryService().createDeploymentQuery().deploymentId(deploymentId).singleResult();
            if (deployment != null) {
                if (!StringUtils.isBlank(deployment.getId())) {
                    Model model = repositoryService.createModelQuery().deploymentId(deploymentId).singleResult();
                    //如果流程节点未配置参与者不允许启动流程
                    hasCompleteAssignment(model.getId());
                    ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                            .deploymentId(deploymentId).singleResult();
                    ProcessInstance processInstance = processEngine.getRuntimeService().startProcessInstanceById(processDefinition.getId(), businessKey);

                    return processInstance.getId() + " : " + processInstance.getProcessDefinitionId();
                } else {
                    throw new ValidateException("the process not published cannot be started。");
                }
            } else {
                throw new ValidateException("cannot find the published model。");
            }
        } else {
            throw new ValidateException("deploymentId and businessKey cannot be null 。");
        }

    }

    @Override
    public boolean completeTask(String processInstanceId) {
        Task task = processEngine.getTaskService().createTaskQuery().processInstanceId(processInstanceId).singleResult();
        logger.info("task {} find ", task.getId());
        processEngine.getTaskService().complete(task.getId());
        return true;
    }

    /**
     * 模型是否有正在执行的流程信息
     *
     * @param modelId 模型id
     * @return 是否正在运行
     */
    @Override
    public boolean isRunning(String modelId) {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        Model model = repositoryService.getModel(modelId);
        if (model.getDeploymentId() != null) {
            ProcessDefinition processDefinition = processEngine.getRepositoryService().createProcessDefinitionQuery().deploymentId(model.getDeploymentId()).singleResult();
            List<Execution> list = processEngine.getRuntimeService().createExecutionQuery().processDefinitionId(processDefinition.getId()).list();
            return list.size() != 0;
        }
        return false;
    }

    /**
     * 显示流程图
     *
     * @param deploymentId 部署id
     * @return base64图片字符
     * @throws IOException io流异常
     */
    @Override
    public String processPic(String deploymentId) throws IOException {
        if (!StringUtils.isBlank(deploymentId)) {
            ProcessDefinition processDefinition = this.processEngine.getRepositoryService().
                    createProcessDefinitionQuery().deploymentId(deploymentId).singleResult();
            BpmnModel bpmnModel = this.processEngine.getRepositoryService().getBpmnModel(processDefinition.getId());
            ProcessDiagramGenerator p = new DefaultProcessDiagramGenerator();
            InputStream is = p.generateDiagram(bpmnModel, "png", Collections.emptyList(), Collections.emptyList(), "宋体", "宋体", "宋体", null, 1.0);
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) != -1) {
                os.write(buffer, 0, len);
            }
            String encode = Base64.encodeBase64String(os.toByteArray());
            os.close();
            is.close();
            return encode;
        } else {
            throw new ValidateException("The process is unpublished and cannot be previewed");
        }
    }

    /**
     * 显示流程图及其高亮
     *
     * @param processInstanceId 流程实例id
     */
    @Override
    public String processDiagramPic(String processInstanceId) throws IOException {
        ProcessInstance pi = this.processEngine.getRuntimeService().createProcessInstanceQuery()
                .processInstanceId(processInstanceId).singleResult();
        BpmnModel bpmnModel = this.processEngine.getRepositoryService().getBpmnModel(pi.getProcessDefinitionId());
        List<String> activeIds = this.processEngine.getRuntimeService().getActiveActivityIds(pi.getId());
        ProcessDiagramGenerator p = new DefaultProcessDiagramGenerator();
        InputStream is = p.generateDiagram(bpmnModel, "png", activeIds, Collections.emptyList(), "宋体", "宋体", "宋体", null, 1.0);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        String encode = Base64.encodeBase64String(os.toByteArray());
        os.close();
        is.close();
        return encode;
    }

    @Override
    public boolean back() {
        return false;
    }

    @Override
    public boolean transfer() {
        return false;
    }

    @Override
    public boolean addRole() {
        return false;
    }

    @Override
    public boolean addUser() {
        return false;
    }

    @Override
    public boolean addGroup() {
        return false;
    }


    /**
     * 获取模型xml
     *
     * @param modelId 模型id
     * @throws IOException IOException
     */


    @Override
    public void exportProcessXml(String modelId, HttpServletResponse response) throws IOException {
        ByteArrayInputStream in = null;
        try {
            RepositoryService repositoryService = processEngine.getRepositoryService();
            processEngine.getIdentityService().createGroupQuery().list();
            Model modelData = repositoryService.getModel(modelId);
            BpmnJsonConverter jsonConverter = new BpmnJsonConverter();
            //获取节点信息
            byte[] arg0 = repositoryService.getModelEditorSource(modelData.getId());
            JsonNode editorNode = new ObjectMapper().readTree(arg0);
            JsonNode jsonNode = editorNode.get("activiti:candidateGroups");
            //将节点信息转换为xml
            BpmnModel bpmnModel = jsonConverter.convertToBpmnModel(editorNode);
            BpmnXMLConverter xmlConverter = new BpmnXMLConverter();
            byte[] bpmnBytes = xmlConverter.convertToXML(bpmnModel);

            in = new ByteArrayInputStream(bpmnBytes);
            IOUtils.copy(in, response.getOutputStream());
//                String filename = bpmnModel.getMainProcess().getId() + ".bpmn20.xml";
            String filename = modelData.getName() + ".bpmn20.xml";
            response.setHeader("Content-Disposition", "attachment; filename=" + java.net.URLEncoder.encode(filename, "UTF-8"));
            response.flushBuffer();
        } catch (Exception e) {
            PrintWriter out = null;
            try {
                out = response.getWriter();
            } catch (IOException e1) {
                e1.printStackTrace();
            } finally {
                if (out != null) {
                    out.close();
                }
            }
            assert out != null;
            out.write("未找到对应数据");
            e.printStackTrace();
        } finally {
            if (in != null) {
                in.close();
            }
        }
    }

    /**
     * 获取模型节点参与者
     *
     * @param modelId 模型id
     * @return List<Assignment>
     * @throws IOException IOException
     */

    @Override
    public List<Assignment> getModelAssignment(String modelId) throws IOException {

        List<Assignment> assignments = new ArrayList<>();
        List<String> candidateGroups;
        List<String> candidateUsers;
        Assignment assignment;
        //获取各节点信息
        byte[] modelEditorSourceExtra = processEngine.getRepositoryService().getModelEditorSource(modelId);
        ObjectNode modelNode = (ObjectNode) new ObjectMapper().readTree(modelEditorSourceExtra);
        JsonNode childShapes = modelNode.findValue("childShapes");
        for (JsonNode childShape : childShapes) {
            JsonNode properties = childShape.findValue("properties");
            //
            JsonNode userTaskAssignment = properties.findValue("usertaskassignment");
            if (userTaskAssignment != null) {
                JsonNode tempAssignment = userTaskAssignment.findValue("assignment");
                JsonNode tempCandidateGroups = tempAssignment.findValue("candidateGroups");
                JsonNode tempCandidateUsers = tempAssignment.findValue("candidateUsers");
                JsonNode tempAssignee = tempAssignment.findValue("assignee");
                assignment = new Assignment();
                candidateGroups = new ArrayList<>();
                candidateUsers = new ArrayList<>();

                //执行人
                String assignee = tempAssignee == null ? null : tempAssignee.asText();
                //候选人组
                if (tempCandidateGroups != null)
                    for (JsonNode candidateGroup : tempCandidateGroups) {
                        if (candidateGroup.findValue("value") != null) {
                            candidateUsers.add(candidateGroup.findValue("value").asText());
                        }

                    }

                //候选人
                if (tempCandidateUsers != null)
                    for (JsonNode candidateUser : tempCandidateUsers) {
                        if (candidateUser.findValue("value") != null) {
                            candidateUsers.add(candidateUser.findValue("value").asText());
                        }

                    }
                assignment.setUserTask(properties.findValue("name").asText()).setAssignee(assignee)
                        .setCandidateUsers(candidateUsers).setCandidateGroups(candidateGroups);
                assignments.add(assignment);
            }
        }

        return assignments;
    }

    @Override
    public List<String> selectTodo() {

        return null;
    }

    @Override
    public List<String> selectDone() {
        return null;
    }

    private void hasCompleteAssignment(String modelId) throws IOException {
        //如果流程节点未配置参与者不允许启动流程
        List<Assignment> modelAssignment = getModelAssignment(modelId);
        for (Assignment assignment : modelAssignment) {
            if (StringUtils.isBlank(assignment.getAssignee())
                    && CollectionUtils.isEmpty(assignment.getCandidateGroups())
                    && CollectionUtils.isEmpty(assignment.getCandidateGroups())) {
                throw new ValidateException("some node are not added assignment");
            }
        }
    }

}
