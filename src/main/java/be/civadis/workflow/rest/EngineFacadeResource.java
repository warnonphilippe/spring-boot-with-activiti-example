package be.civadis.workflow.rest;

import be.civadis.workflow.activiti.EngineFacade;
import be.civadis.workflow.security.AuthoritiesConstants;
import be.civadis.workflow.security.SecurityUtils;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.engine.task.TaskQuery;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.activiti.rest.service.api.runtime.task.TaskResponse;
import org.springframework.data.domain.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * Ressource permettant d'effectuer des opérations sur EngineFacade (ou directement sur l'API workflow si nécessaire)
 * Les opérations peuvent être sécurisée par oauth2
 *
 * Rem :
 *  L'EngineFacade peut être utilisée dans d'autres ressources rest du service afin de combiner action sur les données et sur workflow
 *  L'API rest native de workflow peut être exposée par Spring-boot, mais elle est sécurisée par basic auth
 *  TODO : si nécessaire, affiner secu (par exemple ouvrir au user ordinaire mais restreindre l'accès uniquement à leurs tâches)
 */
@RestController
@RequestMapping("/workflow")
public class EngineFacadeResource {

    private static List<String> properties = new ArrayList<>();

    private EngineFacade engineFacade;
    protected RestResponseFactory restResponseFactory;

    public EngineFacadeResource(EngineFacade engineFacade, RestResponseFactory restResponseFactory) {
        this.engineFacade = engineFacade;
        this.restResponseFactory = restResponseFactory;
    }

    /**
     * Start un process workflow
     * @param processName nom du process
     * @param variables variables d'initialisation
     * @return
     */
    @PostMapping(value = "/processes/{processName}/start")
    //@Secured({AuthoritiesConstants.ADMIN})
    public ResponseEntity<ProcessInstanceResponse> startProcess(@PathVariable String processName, @RequestBody Map<String, Object> variables){
        ProcessInstance processInstance = engineFacade.startProcess(processName, variables);
        return response(this.restResponseFactory.createProcessInstanceResponse(processInstance));
    }

    /**
     * Recherche la liste des tasks pouvant être traitées à un user selon ses groupes,
     * @param groups groupes autorisés à traiter les tâches
     * @param processKey
     * @param processInstanceId
     * @return
     */
    @GetMapping(value = "/tasks-claimable")
    @Secured({AuthoritiesConstants.ADMIN})
    public ResponseEntity<List<TaskResponse>> findClaimableTasks(Pageable pageable,
                                                                 @RequestParam(value="user", required=false) String user,
                                                                 @RequestParam(value="groups", required=false) List<String> groups,
                                                                 @RequestParam(value="processKey", required=false) String processKey,
                                                                 @RequestParam(value="processInstanceId", required=false) String processInstanceId) {

        TaskQuery query = engineFacade.findClaimableTasks(user, groups, processKey, processInstanceId);
        return executeTaskQuery(query, pageable, "/workflow/tasks-claimable");
    }

    /**
     * Recherche la liste des tasks pouvant être traitées à le user courant
     * @param processKey
     * @param processInstanceId
     * @return
     */
    @GetMapping(value = "/my-tasks-claimable")
    @Secured({AuthoritiesConstants.USER})
    public ResponseEntity<List<TaskResponse>> findMyClaimableTasks(Pageable pageable,
                                                                   @RequestParam(value="processKey", required=false) String processKey,
                                                                   @RequestParam(value="processInstanceId", required=false) String processInstanceId) {

        TaskQuery query = engineFacade.findClaimableTasks(getCurrentUser(), getCurrentGroups(), processKey, processInstanceId);
        return executeTaskQuery(query, pageable, "/workflow/my-tasks-claimable");
    }

    /**
     * Recherche la liste des tasks déjà assignées à un user
     * @param user
     * @param processKey
     * @param processInstanceId
     * @return
     */
    @GetMapping(value = "/tasks-assigned")
    @Secured({AuthoritiesConstants.ADMIN})
    public ResponseEntity<List<TaskResponse>> findAssignedTasks(Pageable pageable,
                                                                @RequestParam("user") String user,
                                                                @RequestParam(value="processKey", required=false) String processKey,
                                                                @RequestParam(value="processInstanceId", required=false) String processInstanceId) {

        TaskQuery query = engineFacade.findAssignedTasks(user, processKey, processInstanceId);
        return executeTaskQuery(query, pageable, "/workflow/tasks-assigned");
    }

    /**
     * Recherche la liste des tasks déjà assignées user courant
     * @param processKey
     * @param processInstanceId
     * @return
     */
    @GetMapping(value = "/my-tasks-assigned")
    @Secured({AuthoritiesConstants.USER})
    public ResponseEntity<List<TaskResponse>> findMyAssignedTasks(Pageable pageable,
                                                                  @RequestParam(value="processKey", required=false) String processKey,
                                                                  @RequestParam(value="processInstanceId", required=false) String processInstanceId) {

        TaskQuery query = engineFacade.findAssignedTasks(getCurrentUser(), processKey, processInstanceId);
        return executeTaskQuery(query, pageable, "/workflow/my-tasks-assigned");
    }

    /**
     * Demande l'assignation d'une task à un user
     * @param taskId
     * @param userId
     */
    @PostMapping(value = "/tasks/{taskId}/claim")
    @Secured({AuthoritiesConstants.ADMIN})
    public ResponseEntity<Boolean> claim(@PathVariable("taskId") String taskId, @RequestParam("userId") String userId){
        Task task = engineFacade.findClaimableTask(taskId, null, null);
        if (task != null){
            engineFacade.claim(taskId, userId);
            return response(true);
        }
        return response(false);
    }

    /**
     * Demande l'assignation d'une task au user courant
     * @param taskId
     */
    @PostMapping(value = "/my-tasks/{taskId}/claim")
    @Secured({AuthoritiesConstants.USER})
    public ResponseEntity<Boolean> myClaim(@PathVariable("taskId") String taskId){
        Task task = engineFacade.findClaimableTask(taskId, getCurrentUser(), getCurrentGroups());
        if (task != null){
            engineFacade.claim(taskId, getCurrentUser());
            return response(true);
        }
        return response(false);
    }

    /**
     * Annulation de l'assignation de la task
     * @param taskId
     */
    @PostMapping(value = "/tasks/{taskId}/unclaim")
    @Secured({AuthoritiesConstants.ADMIN})
    public ResponseEntity<Boolean> unclaim(@PathVariable("taskId") String taskId){
        engineFacade.unclaim(taskId);
        return response(true);
    }

    /**
     * Annulation de l'assignation de la task, check si associée au user courant
     * @param taskId
     */
    @PostMapping(value = "/my-tasks/{taskId}/unclaim")
    @Secured({AuthoritiesConstants.USER})
    public ResponseEntity<Boolean> myUnclaim(@PathVariable("taskId") String taskId){
        Task task = engineFacade.findAssignedTask(taskId, getCurrentUser());
        if (task != null){
            engineFacade.unclaim(taskId);
            return response(true);
        }
        return response(false);
    }

    /**
     * Complete une task
     * @param taskId
     * @param params
     */
    @PostMapping(value = "/tasks/{taskId}/complete")
    @Secured({AuthoritiesConstants.ADMIN})
    public ResponseEntity<Boolean> completeTask(@PathVariable("taskId") String taskId, @RequestBody Map<String, Object> params){
        engineFacade.completeTask(taskId, params);
        return response(true);
    }

    /**
     * Complete une task, check si associée au user courant
     * @param taskId
     * @param params
     */
    @PostMapping(value = "/my-tasks/{taskId}/complete")
    @Secured({AuthoritiesConstants.USER})
    public ResponseEntity<Boolean> myCompleteTask(@PathVariable("taskId") String taskId, @RequestBody Map<String, Object> params){
        //check si le user peut traité la tache ou si elle lui est assignée
        Task task = engineFacade.findClaimableTask(taskId, getCurrentUser(), getCurrentGroups());
        if (task == null){
            task = engineFacade.findAssignedTask(taskId, getCurrentUser());
        }
        //si ok, complete task
        if (task != null){
            engineFacade.completeTask(taskId, params);
            return response(true);
        }
        return response(false);
    }

    private ResponseEntity<List<TaskResponse>> executeTaskQuery(TaskQuery query, Pageable pageable, String url){

        //sort
        if (pageable != null && pageable.getSort() != null){

            Iterator<Sort.Order> iterator = pageable.getSort().iterator();
            while(iterator.hasNext()){
                Sort.Order order = iterator.next();
                String prop = order.getProperty();

                boolean asc = true;
                if (order.getDirection() != null && order.getDirection().isDescending()){
                    asc = false;
                }

                boolean ordered = true;

                if ("id".equalsIgnoreCase(prop)){
                    query.orderByTaskId();
                } else if ("name".equalsIgnoreCase(prop)){
                    query.orderByTaskName();
                } else if ("description".equalsIgnoreCase(prop)){
                    query.orderByTaskDescription();
                } else if ("dueDate".equalsIgnoreCase(prop)){
                    query.orderByTaskDueDate();
                } else if ("createTime".equalsIgnoreCase(prop)){
                    query.orderByTaskCreateTime();
                } else if ("priority".equalsIgnoreCase(prop)){
                    query.orderByTaskPriority();
                } else if ("executionId".equalsIgnoreCase(prop)){
                    query.orderByExecutionId();
                } else if ("tenantId".equalsIgnoreCase(prop)){
                    query.orderByTenantId();
                } else if ("definitionKey".equalsIgnoreCase(prop)){
                    query.orderByTaskDefinitionKey();
                } else if ("assignee".equalsIgnoreCase(prop)){
                    query.orderByTaskAssignee();
                } else if ("owner".equalsIgnoreCase(prop)){
                    query.orderByTaskOwner();
                } else if ("processInstanceId".equalsIgnoreCase(prop)){
                    query.orderByProcessInstanceId();
                } else if ("processDefinitionId".equalsIgnoreCase(prop)) {
                    query.orderByProcessDefinitionId();
                } else {
                    ordered = false;
                }

                if (ordered){
                    if (asc){
                        query.asc();
                    } else {
                        query.desc();
                    }
                }

            }

        }

        //query
        List<Task> taskList = query.listPage(pageable.getOffset(), pageable.getPageSize());

        //response
        return response(
                this.restResponseFactory.createTaskResponseList(taskList));
    }

    private <T> ResponseEntity<T> response(T entity){
        return ResponseEntity.ok()
                .body(entity);
    }

    private <T> ResponseEntity<T> response(HttpHeaders headers, T entity){
        return ResponseEntity.ok()
                .headers(headers)
                .body(entity);
    }

    private String getCurrentUser(){
        return SecurityUtils.getCurrentUserLogin().orElse("");
    }

    private List<String> getCurrentGroups(){
        //REM : Nous ne définissons pas de user / group dans la secu d'workflow, nous utilisons les users et roles oauth2
        return SecurityUtils.getRoles();
    }


}
