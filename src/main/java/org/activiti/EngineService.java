package org.activiti;

import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class EngineService {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    public EngineService() {
    }

    public ProcessInstance startProcess(String processName, Map<String, Object> variables){
        return runtimeService.startProcessInstanceByKey(processName, variables);
    }

    public List<Task> findTasksByCriteria(TaskCriteria criteria){

        //TODO : compléter la recherche, synchro users activiti et usrs du service ?

        TaskQuery query = taskService.createTaskQuery();

        if (criteria.getUser() != null){
            query.taskCandidateOrAssigned(criteria.getUser());
        }

        if (criteria.getProcessDefinitionId() != null){
            query.processDefinitionId(criteria.getProcessDefinitionId());
        }

        if (criteria.getProcessInstanceId() != null){
            query.processInstanceId(criteria.getProcessInstanceId());
        }

        return query.list();


    }

    public Task getTasksByGroup(String processInstanceId, String group){
        return taskService.createTaskQuery()
                .processInstanceId(processInstanceId)
                .taskCandidateGroup(group)
                .singleResult();

    }

    public void completeTask(String taskId, Map<String, Object> params){
        taskService.complete(taskId, params);
    }


}
