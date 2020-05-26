package com.tianheng.workflow.service;

import org.activiti.engine.repository.Model;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public interface WFService {
    /**
     * 新建一个空模型
     */
    void createModel(HttpServletResponse response) throws IOException;

    /**
     * 获取所有模型
     */
    List<Model> selectModels();

    /**
     * 发布/移除 模型为流程定义
     */
    boolean publish(String modelId) throws IOException;

    /**
     * 启动流程
     */
    String startProcess(String deploymentId);

    /**
     * 提交任务
     */
    boolean completeTask(String processInstanceId);
}
