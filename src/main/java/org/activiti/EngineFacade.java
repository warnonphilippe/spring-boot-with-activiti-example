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
public class EngineFacade {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    public EngineFacade() {
    }

    public ProcessInstance startProcess(String processName, Map<String, Object> variables){
        return runtimeService.startProcessInstanceByKey(processName, variables);
    }

    //TODO : compléter la recherche,
    // synchro users activiti et usrs du service ou decl. des strings dans tâches suffit ?
    // TODO : comment travailler alors avec des groupes ?


    /**
     * Recherche la liste des tasks déjà assignées à un user
     * @param user
     * @param processKey
     * @param processInstanceId
     * @return
     */
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

    /**
     * Recherche la liste des tasks pouvant être traitées par un user,
     * il devra alors demander que la task lui soit assignée avant de la traitée
     * @param groups
     * @param processKey
     * @param processInstanceId
     * @return
     */
    public List<Task> findClaimableTasks(List<String> groups, String processKey, String processInstanceId){

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

    /**
     * Recherche les tâches assignées au user et les tâches dont il peut demander l'assignation
     * Une tâche peut avoir été assignée par le workflow ou suite à une demande d'assignation
     * Un user peut demander l'assignation d'une tâche
     *      s'il est déclaré dans les candidats de la tâche
     *      ou s'il est membre d'un des groupes associés à la tâche
     * @param groups
     * @param processKey
     * @param processInstanceId
     * @return
     */
    public List<Task> findTasks(String user, List<String> groups, String processKey, String processInstanceId) {

        TaskQuery query = taskService.createTaskQuery().or();

        if (user != null){
            query.taskAssignee(user);
        }

        if (groups != null && !groups.isEmpty()){
            query.taskCandidateGroupIn(groups);
        }

        query.endOr();

        if (processKey != null){
            query.processDefinitionId(processKey);
        }

        if (processInstanceId != null){
            query.processInstanceId(processInstanceId);
        }

        return query.list();

    }

    /**
     * Indique si une tâche est assignée à un user
     * @param task
     * @param userId
     * @return
     */
    public boolean isAssigned(Task task, String userId){
        return task.getAssignee().equalsIgnoreCase(userId);
    }

    /**
    * Demande l'assignation d'une task à un user
    * @param taskId
    * @param userId
    */
    public void claim(String taskId, String userId){
        taskService.claim(taskId, userId);
    }

    /**
     * Annulation de l'assignation de la task
     * @param taskId
     */
    public void unclaim(String taskId){
        taskService.unclaim(taskId);
    }

    /**
     * Complete une task
     * @param taskId
     * @param params
     */
    public void completeTask(String taskId, Map<String, Object> params){
        taskService.complete(taskId, params);
    }


}
