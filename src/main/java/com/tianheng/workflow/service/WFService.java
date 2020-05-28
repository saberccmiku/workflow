package com.tianheng.workflow.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.tianheng.workflow.entity.Assignment;
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
     * 新建一个空模型
     */
    boolean delModel(String modelId);


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
    String startProcess(String deploymentId, String businessKey) throws IOException;

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

    /**
     * 回退
     *
     * @return 是否成功
     */
    boolean back();

    /**
     * 转交
     * 直接将办理人assignee 换成别人，这时任务的拥有着不再是转办人，而是为空，相当与将任务转出。
     *
     * @param taskId 任务id
     * @param userId 被委派人id
     * @return 是否成功
     */
    boolean transfer(String taskId, String userId);

    /**
     * 委派：是将任务节点分给其他人处理，等其他人处理好之后，委派任务会自动回到委派人的任务中
     *
     * @param taskId 任务id
     * @param userId 被委派人id
     */
    boolean delegate(String taskId, String userId);

    /**
     * 被委派人办理任务后,回到委派人
     *
     * @param taskId taskId 任务id
     */

    boolean resolveTask(String taskId);

    /**
     * 添加角色
     *
     * @return 是否成功
     */
    boolean addRole();

    /**
     * 添加参与者
     *
     * @return 是否成功
     */
    boolean addUser();

    /**
     * 添加用户组
     *
     * @return 是否成功
     */
    boolean addGroup();

    /**
     * 导出流程xml配置
     */
    void exportProcessXml(String modelId, HttpServletResponse response) throws IOException;

    /**
     * 获取模型参与者
     *
     * @param modelId 模型id
     * @return 参与者
     * @throws IOException 异常
     */
    List<Assignment> getModelAssignment(String modelId) throws IOException;

    /**
     * 查询待办
     *
     * @return
     */
    List<String> selectTodo();

    /**
     * 查询已办
     *
     * @return
     */
    List<String> selectDone();

}
