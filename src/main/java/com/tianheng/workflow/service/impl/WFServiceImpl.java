package com.tianheng.workflow.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
    public List<Model> selectModels() {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        return repositoryService.createModelQuery().list();
    }

    @Override
    public boolean deploy(String modelId) throws IOException {

        if (StringUtils.isEmpty(modelId)) {
            throw new ValidateException("modelId不能为空");
        }
        //获取模型
        RepositoryService repositoryService = processEngine.getRepositoryService();
        Model modelData = repositoryService.getModel(modelId);
        if (modelData != null) {
            if (StringUtils.isBlank(modelData.getDeploymentId())) {//deploymentId不为空则移除发布的流程，否则发布流程
                byte[] bytes = repositoryService.getModelEditorSource(modelData.getId());
                if (bytes == null) {
                    throw new ValidateException("模型数据为空，请先设计流程并成功保存，再进行发布。");
                }
                JsonNode modelNode = new ObjectMapper().readTree(bytes);

                BpmnModel model = new BpmnJsonConverter().convertToBpmnModel(modelNode);
                if (model.getProcesses().size() == 0) {
                    throw new ValidateException("数据模型不符要求，请至少设计一条主线流程。");
                }
                byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(model);

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
            throw new ValidateException("模型不存在。");
        }
        return true;
    }

    @Override
    public String startProcess(String deploymentId) {
        RepositoryService repositoryService = processEngine.getRepositoryService();
        //获取模型
        Model model = repositoryService.getModel(deploymentId);
        if (model != null) {
            if (!StringUtils.isBlank(model.getId())) {
                ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                        .deploymentId(deploymentId).singleResult();
                ProcessInstance processInstance = processEngine.getRuntimeService().startProcessInstanceById(processDefinition.getId());
                return processInstance.getId() + " : " + processInstance.getProcessDefinitionId();
            } else {
                throw new ValidateException("流程未发布不能启动。");
            }
        } else {
            throw new ValidateException("模型不存在。");
        }

    }

    @Override
    public boolean completeTask(String processInstanceId) {
        Task task = processEngine.getTaskService().createTaskQuery().processInstanceId(processInstanceId).singleResult();
        logger.info("task {} find ", task.getId());
        processEngine.getTaskService().complete(task.getId());
        return true;
    }
}
