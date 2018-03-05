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
        return runtimeService.startProcessInstanceByKeyAndTenantId(processName, variables, getCurrentTenant());
    }

    // TODO : compléter la recherche,
    // TODO :  A tester, synchro users activiti et usrs du service ou decl. des strings dans tâches suffit ?


    /**
     * Dans la config des processus, les tâches seront associées à des candidates-groups
     * Le user connecté pourra donc accéder aux tâches
     *  - associées à des groupes dont il est membre
     *  - lui étant assignées
     * Avant de traiter une tâche, le tâche devra être assignée au user
     *  - soit lors de sas définition
     *  - soit en faisant un claim d'une tâche associé à un groupe dont il est membre
     *
     */

    /**
     * Recherche la liste des tasks pouvant être traitées à un user selon ses groupes,
     * il devra alors demander que la task lui soit assignée avant de la traitée (cfr claim)
     * @param groups groupes autorisés à traiter les tâches
     * @param processKey
     * @param processInstanceId
     * @return
     */
    public TaskQuery findClaimableTasks(String user, List<String> groups, String processKey, String processInstanceId){

        TaskQuery query = taskService.createTaskQuery().taskTenantId(getCurrentTenant());

        if (user != null){
            query.taskCandidateUser(user);
        }

        if (groups != null && !groups.isEmpty()){
            query.taskCandidateGroupIn(groups);
        }

        if (processKey != null){
            query.processDefinitionId(processKey);
        }

        if (processInstanceId != null){
            query.processInstanceId(processInstanceId);
        }

        return query;

    }

    /**
     * Recherche la liste des tasks déjà assignées à un user
     * @param user
     * @param processKey
     * @param processInstanceId
     * @return
     */
    public TaskQuery findAssignedTasks(String user, String processKey, String processInstanceId){

        TaskQuery query = taskService.createTaskQuery();

        query.taskAssignee(user);

        if (processKey != null){
            query.processDefinitionId(processKey);
        }

        if (processInstanceId != null){
            query.processInstanceId(processInstanceId);
        }

        return query;

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
    public TaskQuery findTasks(String user, List<String> groups, String processKey, String processInstanceId) {

        TaskQuery query = taskService.createTaskQuery().taskTenantId(getCurrentTenant()).or();

        //taches assignées au user
        if (user != null){
            query.taskAssignee(user);
            query.taskCandidateUser(user);
            //travail avec les groupes définis dans acticiti, on ne les utilise pas
            //query.taskCandidateOrAssigned(user);
        }

        //taches dont il peut demander l'assignation
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

        return query;

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

    private String getCurrentTenant(){
        return TenantUtils.getCurrentTenant();
    }

}
