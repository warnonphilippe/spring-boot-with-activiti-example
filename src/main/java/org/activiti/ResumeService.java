package org.activiti;

import org.springframework.stereotype.Component;

/**
 * Modele pour la demo du processus activiti hireProcessWithJpa
 */
@Component
public class ResumeService {

    //TODO : voir comment transmettre paramètres...
    // voir https://www.activiti.org/userguide/index.html#springSpringBoot
    public void storeResume() {
        System.out.println("Storing resume ...");
    }

}
