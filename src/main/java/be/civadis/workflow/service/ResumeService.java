package be.civadis.workflow.service;

import org.springframework.stereotype.Component;

/**
 * Modele pour la demo du processus workflow hireProcessWithJpa
 */
@Component
public class ResumeService {

    //TODO : voir comment transmettre paramètres...
    // voir https://www.activiti.org/userguide/index.html#springSpringBoot
    public void storeResume() {
        System.out.println("Storing resume ...");
    }

}
