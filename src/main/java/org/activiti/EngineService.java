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

    //TODO : compléter la recherche,
    // synchro users activiti et usrs du service ou decl. des strings dans tâches suffit ?
    // TODO : mais comment travailler alors avec des groupes ?


    public List<Task> findAssignedTasks(String user, String processKey, String processInstanceId){

        TaskQuery query = taskService.createTaskQuery();

        query.taskAssignee(user);

        if (processKey != null){
            query.processDefinitionId(processKey);
        }

        if (processInstanceId != null){
            query.processInstanceId(processInstanceId);
        }

        return query.list();

    }

    public List<Task> findTasks(List<String> groups, String processKey, String processInstanceId){

        TaskQuery query = taskService.createTaskQuery();

        //soit on recherche les tâches que le user courant peut demander (selon ses groupes)
        if (groups != null && !groups.isEmpty()){
            query.taskCandidateGroupIn(groups);
        }

        if (processKey != null){
            query.processDefinitionId(processKey);
        }

        if (processInstanceId != null){
            query.processInstanceId(processInstanceId);
        }

        return query.list();

    }

    public void claim(String taskId, String userId){
        taskService.claim(taskId, userId);
    }

    public void unclaim(String taskId){
        taskService.unclaim(taskId);
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
