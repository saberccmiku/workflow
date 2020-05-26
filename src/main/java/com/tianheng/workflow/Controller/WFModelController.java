package com.tianheng.workflow.Controller;

import com.tianheng.workflow.service.WFService;
import com.tianhengyun.common.tang4jbase.support.ResponseModel;
import com.tianhengyun.common.tang4jbase.support.ResponseModelFactory;
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
        return ResponseModelFactory.OKWithData(wfService.selectModels());
    }

    /**
     * 发布/移除 模型为流程定义
     */

    @PutMapping("/publish/models/{modelId}")
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
    @PostMapping("/procDef/startProcess/{deploymentId}")
    public ResponseModel startProcess(@PathVariable String deploymentId) {
        try {
            return ResponseModelFactory.OKWithData(wfService.startProcess(deploymentId));
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


}