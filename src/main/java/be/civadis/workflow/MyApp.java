package be.civadis.workflow;

import be.civadis.workflow.activiti.ActivitiProcessImporter;
import be.civadis.workflow.security.TenantUtils;
import org.activiti.engine.IdentityService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.User;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;


@SpringBootApplication
public class MyApp {

    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }

    /**
     * Crée un bean d'initialisation qui va importer les définitions de processus du répertoire resources/processes dans workflow
     * Par exemple, si les tenants sont tenant1 et tenant2 (définis dans la config de l'application, propriété schemas)
     *  -> le répertoire resources/processes doit alors être structuté comme ceci:
     *      /all : les fichiers seront importés et associés à tenant1 et tenant2
     *      /tenant1 : fichiers importés uniquement pour tenant1
     *      /tenant2 : fichiers importés uniquement pour tenant2
     *  Par défaut,
     *      spring boot importe tous les fichiers à la racine de processes, mais sans les associés aux tenants,
     *      celà ne convient donc que dans un environnement single tenant
     * @param importer
     * @return
     */

    @Bean
    InitializingBean deploy(final ActivitiProcessImporter importer) {

        return new InitializingBean() {
            public void afterPropertiesSet() throws Exception {

                if (TenantUtils.getTenants()!= null){

                    // import des processus généraux (sous le répertoire /all)
                    importer.importFiles("processes/all", TenantUtils.getTenants());

                    //import des processus spécifiques (sous les répertoires associés à des tenants)
                    for (String tenant : TenantUtils.getTenants()){
                        importer.importFiles("processes/" + tenant, Arrays.asList(tenant));
                    }
                }
            }
        };
    }


    // pour demo
    //@Bean
    InitializingBean deployHiring(final RepositoryService repositoryService) {

        return new InitializingBean() {
            public void afterPropertiesSet() throws Exception {

                try {
                    DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();


                    String fileName = "processes/all/Developer_Hiring_with_jpa.bpmn20.xml";

                    ClassLoader classLoader = getClass().getClassLoader();
                    File file = new File(classLoader.getResource(fileName).getFile());

                    deploymentBuilder.addInputStream(fileName, new FileInputStream(file));

                    deploymentBuilder.name(fileName);
                    deploymentBuilder.tenantId("jhipster");

                    Deployment deployment = deploymentBuilder.deploy();

                } catch (Exception ex) {
                    ex.printStackTrace();
                    throw ex;
                }

            }

        };


    }

    /**
     * Création de groupe et user pour la basic auth de l'api rest native workflow
     * @param identityService
     * @return
     */
    // pour demo,
    //@Bean
    InitializingBean usersAndGroupsInitializer(final IdentityService identityService) {
        // TODO : si besoin de l'api en prod, créer les groupes et users sécurisés (ldap, db,... ?)
        return new InitializingBean() {
            public void afterPropertiesSet() throws Exception {

                Group group = identityService.newGroup("user");
                group.setName("users");
                group.setType("security-role");
                identityService.saveGroup(group);

                User admin = identityService.newUser("admin");
                admin.setPassword("admin");
                identityService.saveUser(admin);

            }
        };
    }

}
