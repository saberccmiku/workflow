package com.tianheng.workflow.Controller;

import com.tianheng.workflow.entity.req.ReqStartProcess;
import com.tianheng.workflow.service.WFService;
import com.tianhengyun.common.tang4jbase.support.RequestPage;
import com.tianhengyun.common.tang4jbase.support.ResponseModel;
import com.tianhengyun.common.tang4jbase.support.ResponseModelFactory;
import org.activiti.engine.repository.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
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
    @RequestMapping("/completeTask")
    public ResponseModel completeTask(@RequestBody String processInstanceId) {
        return ResponseModelFactory.OKWithData(wfService.completeTask(processInstanceId));
    }

    /**
     * 测试接口
     */
    @GetMapping("/test/{modelId}")
    public ResponseModel test(@PathVariable String modelId) {
        return ResponseModelFactory.OKWithData(wfService.isRunning(modelId));
    }


}