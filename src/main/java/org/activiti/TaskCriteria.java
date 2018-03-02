package org.activiti;

import java.util.List;

public class TaskCriteria {


    private String user;
    private List<String> groups;
    private String processInstanceId;
    private String processKey;

    public TaskCriteria() {
    }

    public String getUser() {
        return user;
    }

    public List<String> getGroups() {
        return groups;
    }

    public void setGroups(List<String> groups) {
        this.groups = groups;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getProcessKey() {
        return processKey;
    }

    public void setProcessKey(String processKey) {
        this.processKey = processKey;
    }
}


