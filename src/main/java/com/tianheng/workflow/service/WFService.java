package com.tianheng.workflow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
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
    List<Model> models();

    /**
     * 获取所有模型分页
     */
    IPage<Model> pageModels(int page, int size);

    /**
     * 发布/移除 模型为流程定义
     */
    boolean publish(String modelId) throws IOException;

    /**
     * 启动流程
     */
    String startProcess(String deploymentId,String businessKey);

    /**
     * 提交任务
     */
    boolean completeTask(String processInstanceId);

    /**
     * 模型是否有正在执行的流程信息
     *
     * @param modelId 模型id
     * @return 是否正在运行
     */
    boolean isRunning(String modelId);

    /**
     * 显示流程图
     */
    String processPic(String deploymentId) throws IOException;

    /**
     * 显示流程图及其高亮
     *
     * @param processInstanceId 流程实例id
     */
    String processDiagramPic(String processInstanceId) throws IOException;

}
