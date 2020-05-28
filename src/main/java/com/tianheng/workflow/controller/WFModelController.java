package com.tianheng.workflow.controller;

import com.tianheng.workflow.entity.req.GroupTaskId;
import com.tianheng.workflow.entity.req.ReqStartProcess;
import com.tianheng.workflow.entity.req.ReqTaskInfo;
import com.tianheng.workflow.service.WFService;
import com.tianhengyun.common.tang4jbase.support.RequestPage;
import com.tianhengyun.common.tang4jbase.support.ResponseModel;
import com.tianhengyun.common.tang4jbase.support.ResponseModelFactory;
import org.activiti.engine.repository.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.groups.Default;
import java.io.IOException;

@RestController
@RequestMapping("/workflow")
public class WFModelController {

    private static final Logger logger = LoggerFactory.getLogger(WFModelController.class);

    @Autowired
    private WFService wfService;

    /**
     * 新建一个空模型
     */
    @PostMapping("/create")
    public void create(HttpServletResponse response) throws IOException {
        wfService.createModel(response);
    }

    /**
     * 删除模型
     */
    @PostMapping("/del/{modelId}")
    public ResponseModel create(@PathVariable String modelId) {
        return ResponseModelFactory.OKWithData(wfService.delModel(modelId));
    }


    /**
     * 获取所有模型
     */
    @GetMapping("/models")
    public ResponseModel models() {
        return ResponseModelFactory.OKWithData(wfService.models());
    }


    /**
     * 获取所有模型分页
     */
    @GetMapping("/pageModels")
    public ResponseModel pageModels(@RequestBody RequestPage<Model> requestPage) {
        return ResponseModelFactory.OKWithData(wfService.pageModels(requestPage.getCurrent(), requestPage.getSize()));
    }


    /**
     * 发布/移除 模型为流程定义
     */

    @PutMapping("/publish/{modelId}")
    public ResponseModel publish(@PathVariable String modelId) {
        try {
            return ResponseModelFactory.OKWithData(wfService.publish(modelId));
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
            return ResponseModelFactory.error(e.getMessage());
        }

    }

    /**
     * 根据发布id启动流程
     */
    @PostMapping("/procDef/startProcess")
    public ResponseModel startProcess(@RequestBody ReqStartProcess request) {
        try {
            return ResponseModelFactory.OKWithData(wfService.startProcess(request.getDeploymentId(), request.getBusinessKey()));
        } catch (Exception e) {
            return ResponseModelFactory.error(e.getMessage());
        }

    }

    /**
     * 提交任务
     */
    @PostMapping("/completeTask/{processInstanceId}")
    public ResponseModel completeTask(@PathVariable String processInstanceId) {
        return ResponseModelFactory.OKWithData(wfService.completeTask(processInstanceId));
    }

    /**
     * 转交
     * 直接将办理人assignee 换成别人，这时任务的拥有着不再是转办人，而是为空，相当与将任务转出。
     *
     * @param request taskId 任务id userId 被委派人id
     * @return 是否成功
     */
    @PostMapping("/transfer")
    public ResponseModel transfer(@Validated @RequestBody ReqTaskInfo request) {
        try {
            return ResponseModelFactory.OKWithData(wfService.transfer(request.getTaskId(), request.getUserId()));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return ResponseModelFactory.error(e.getMessage());
        }

    }

    /**
     * 委派：是将任务节点分给其他人处理，等其他人处理好之后，委派任务会自动回到委派人的任务中
     *
     * @param request taskId 任务id userId 被委派人id
     */
    public ResponseModel delegate(@Validated({GroupTaskId.class, Default.class}) @RequestBody ReqTaskInfo request) {
        try {
            return ResponseModelFactory.OKWithData(wfService.delegate(request.getTaskId(), request.getUserId()));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return ResponseModelFactory.error(e.getMessage());
        }

    }

    /**
     * 被委派人办理任务后,回到委派人
     *
     * @param request taskId 任务id
     */

    public ResponseModel resolveTask(@Validated({GroupTaskId.class}) @RequestBody ReqTaskInfo request) {
        try {
            return ResponseModelFactory.OKWithData(wfService.resolveTask(request.getTaskId()));
        } catch (Exception e) {
            logger.error(e.getMessage());
            return ResponseModelFactory.error(e.getMessage());
        }

    }

    /**
     * 测试接口
     */
    @GetMapping("/test/{modelId}")
    public ResponseModel test(@PathVariable String modelId) throws IOException {
        return ResponseModelFactory.OKWithData(wfService.getModelAssignment(modelId));
    }


}