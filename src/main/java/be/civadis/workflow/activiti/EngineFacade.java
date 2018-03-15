package be.civadis.workflow.activiti;

import be.civadis.workflow.security.TenantUtils;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Facade exposant les opérations courantes sur l'API workflow
 * Les droits d'accès dépendent de la logique métier de l'application, ses rôles et ses mécanismes de secu
 * Les vérifications doivent donc être effectuées en amont des appels à la facade qui n'est qu'une abstraction technique de l'API workflow
 * La facade pourrait être intégrée à d'autres projets ayant une logique métier différente
 */
@Component
public class EngineFacade {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    public EngineFacade() {
    }

    /**
     * Dans la config des processus, les tâches sont associées à
     *  - candidates-groups : groupes pouvant traiter la task
     *  - candidate-users : users pouvant traiter la task
     *  - assignee : user auquel la task a été assignée
     *
     *  Une tache est lors de sa définition associée à une liste user ou liste de groupes pouvant la traiter
     *  Une tache peut faire l'objet d'un claim d'un des users associés (ou via ses groupes), elle est alors verrouillée pour lui
     *
     * Le user connecté pourra donc accéder aux tâches, soit
     *  - s'il est un user candidat
     *  - s'il est membre d'un des candidate-groups
     *  - si la task lui est assignée
     * Et que la tache n'a pas été assignée à un autre user
     *
     * REM : Nous ne définissons pas de user / group dans la secu d'workflow, nous utilisons les users et roles oauth2
     * Les roles étant plus fin que les groupes, associer des tâches workflow à des roles et non des groupes du user offre plus de souplesse
     *
     */

    /**
     * Démarrer un processus
     * @param processName nom du processus
     * @param variables variables d'initialisation
     * @return
     */
    public ProcessInstance startProcess(String processName, Map<String, Object> variables){
        return runtimeService.startProcessInstanceByKeyAndTenantId(processName, variables, getCurrentTenant());
    }

    /**
     * Recherche la liste des tasks non assignée selon candidate-users, candidate-groups
     * Une task sera retenue soit :
     *  - le user transmis est un des candidate-users
     *  - un des groupes transmis est parmis les candidate-groups
     * Permet de rechercher des task pouvant être traitées par un user ou des groupes
     * @param user
     * @param groups
     * @param processKey
     * @param processInstanceId
     * @return
     */
    public TaskQuery findClaimableTasks(String user, List<String> groups, String processKey, String processInstanceId){

        TaskQuery query = taskService.createTaskQuery()
                .taskTenantId(getCurrentTenant());

        query.taskUnassigned();

        if (user != null || (groups != null && !groups.isEmpty())){

            query = query.or();

            //taches dont il peut demander l'assignation
            if (user != null){
                query.taskCandidateUser(user);
            }

            //taches associées aux groupes
            if (groups != null && !groups.isEmpty()){
                query.taskCandidateGroupIn(groups);
            }

            query = query.endOr();

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

        TaskQuery query = taskService.createTaskQuery()
                .taskTenantId(getCurrentTenant());

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
     * Recherche une tâche non-assignée selon son id et vérifie que la task respecte une des conditions ci-dessous :
     *  - le user transmis est un des candidate-users
     *  - un des groupes transmis est parmis les candidate-groups
     * @param taskId id de la tâche, requis
     * @param user
     * @param groups
     * @return
     */
    public Task findClaimableTask(String taskId, String user, List<String> groups){
        TaskQuery query = taskService.createTaskQuery()
                .taskTenantId(getCurrentTenant())
                .taskId(taskId);

        query.taskUnassigned();

        if (user != null || (groups != null && !groups.isEmpty())){

            query = query.or();

            //taches dont il peut demander l'assignation
            if (user != null){
                query.taskCandidateUser(user);
            }

            //taches dont il peut demander l'assignation (par ses groupes)
            if (groups != null && !groups.isEmpty()){
                query.taskCandidateGroupIn(groups);
            }

            query = query.endOr();

        }

        return query.singleResult();
    }

    /**
     * Recherche une tâche selon son id et vérifie qu'elle est assignée à l'assignee transmis
     *  - le user transmis est un des candidate-users
     *  - un des groupes transmis est parmis les candidate-groups
     *  - la task a été assignée au user transmis
     * @param taskId id de la tâche, requis
     * @param assignee si fourni, vérifie que la task est assignée à l'assignee
     * @return
     */
    public Task findAssignedTask(String taskId, String assignee){
        TaskQuery query = taskService.createTaskQuery()
                .taskTenantId(getCurrentTenant())
                .taskId(taskId)
                .taskAssignee(assignee);
        return query.singleResult();
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
     * Attention, ne pas oublier de vérifier en amont que l'on peut effectuer le claim, dépend des roles et de la secu de l'application
     * @param taskId
     * @param userId
     */
    public void claim(String taskId, String userId){
        taskService.claim(taskId, userId);
    }

    /**
     * Annulation de l'assignation de la task
     * Attention, ne pas oublier de vérifier en amont que l'on peut effectuer le claim, dépend des roles et de la secu de l'application
     * @param taskId
     */
    public void unclaim(String taskId){
        taskService.unclaim(taskId);
    }

    /**
     * Complete une task
     * Attention, ne pas oublier de vérifier en amont que l'on peut effectuer le claim, dépend des roles et de la secu de l'application
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
