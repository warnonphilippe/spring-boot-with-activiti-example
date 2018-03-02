package org.activiti;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;

@RestController
public class HireProcessRestController {

    @Autowired
    private EngineFacade engineFacade;

    @Autowired
    private ApplicantRepository applicantRepository;

    @ResponseStatus(value = HttpStatus.OK)
    @RequestMapping(value = "/start-hire-process", method = RequestMethod.POST,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public void startHireProcess(@RequestBody Map<String, String> data) {

        Applicant applicant = new Applicant(data.get("name"), data.get("email"), data.get("phoneNumber"));
        applicantRepository.save(applicant);

        Map<String, Object> vars = Collections.<String, Object>singletonMap("applicant", applicant);
        engineFacade.startProcess("hireProcessWithJpa", vars);
        //runtimeService.startProcessInstanceByKey("hireProcessWithJpa", vars);
    }

    // exemples CURL

    // start process
    // curl -u admin:admin -H "Content-Type: application/json" -d '{"name":"John Doe", "email": "john.doe@alfresco.com", "phoneNumber":"123456789"}' http://localhost:8080/start-hire-process

    // read tasks
    // curl -u admin:admin -H "Content-Type: application/json" http://localhost:8080/runtime/tasks
    // | python -m json.tool

    // complete tasks
    // curl -u admin:admin -H "Content-Type: application/json" -d '{"action" : "complete", "variables": [ {"name":"telephoneInterviewOutcome", "value":true} ]}' http://localhost:8080/runtime/tasks/14

    // TODO appel de l'API par ressource du microservice, par exemple pour
    // rechercher tâches du users courant et/ou d'un type donné
    // appeler le complétude d'une tâche lorsque l'on enregistre un formulaire associé à la tâche
    // etc...
    // voir class EngineService, class de test,...

}