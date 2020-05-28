package com.tianheng.workflow.entity.req;

import javax.validation.constraints.NotNull;

public class ReqTaskInfo {
    /**
     * 任务id
     */
    @NotNull(groups = {GroupTaskId.class}, message = "taskId cannot be null")
    private String taskId;
    /**
     * 参与者
     */
    private String userId;

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
