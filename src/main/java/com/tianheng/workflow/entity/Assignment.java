package com.tianheng.workflow.entity;

import java.util.List;

public class Assignment {

    /**
     * 节点名称
     */
    private String userTask;

    /**
     * 节点执行者
     */
    private String assignee;
    /**
     * 节点候选人
     */
    private List<String> candidateUsers;
    /**
     * 节点候选组
     */
    private List<String> candidateGroups;

    public String getUserTask() {
        return userTask;
    }

    public Assignment setUserTask(String userTask) {
        this.userTask = userTask;
        return this;
    }

    public String getAssignee() {
        return assignee;
    }

    public Assignment setAssignee(String assignee) {
        this.assignee = assignee;
        return this;
    }

    public List<String> getCandidateUsers() {
        return candidateUsers;
    }

    public Assignment setCandidateUsers(List<String> candidateUsers) {
        this.candidateUsers = candidateUsers;
        return this;
    }

    public List<String> getCandidateGroups() {
        return candidateGroups;
    }

    public Assignment setCandidateGroups(List<String> candidateGroups) {
        this.candidateGroups = candidateGroups;
        return this;
    }
}
