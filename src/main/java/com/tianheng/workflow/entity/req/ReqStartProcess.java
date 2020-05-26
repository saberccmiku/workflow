package com.tianheng.workflow.entity.req;

import javax.validation.constraints.NotNull;

public class ReqStartProcess {

    /**
     * 流程部署id
     */
    @NotNull(message = "deploymentId cannot be null")
    private String deploymentId;
    /**
     * 业务id
     */
    @NotNull(message = "businessKey cannot be null")
    private String businessKey;

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }
}
