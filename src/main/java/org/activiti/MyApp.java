package org.activiti;

import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.IdentityService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.User;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;

import java.io.File;
import java.io.FileInputStream;
import java.util.zip.ZipInputStream;

@SpringBootApplication
public class MyApp {

    public static void main(String[] args) {
        SpringApplication.run(MyApp.class, args);
    }

//    @Bean
//    public CommandLineRunner init(final RepositoryService repositoryService,
//                                  final RuntimeService runtimeService,
//                                  final TaskService taskService) {
//
//        return new CommandLineRunner() {
//            @Override
//            public void run(String... strings) throws Exception {
//                Map<String, Object> variables = new HashMap<String, Object>();
//                variables.put("applicantName", "John Doe");
//                variables.put("email", "john.doe@activiti.com");
//                variables.put("phoneNumber", "123456789");
//                runtimeService.startProcessInstanceByKey("hireProcess", variables);
//            }
//        };
//
//    }

    @Bean
    InitializingBean usersAndGroupsInitializer(final IdentityService identityService) {

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

    @Bean
    InitializingBean deploy(final RepositoryService repositoryService) {

        return new InitializingBean() {
            public void afterPropertiesSet() throws Exception {

                try {
                    DeploymentBuilder deploymentBuilder = repositoryService.createDeployment();


                    String fileName = "Developer_Hiring_with_jpa.bpmn20.xml";

                    ClassLoader classLoader = getClass().getClassLoader();
                    File file = new File(classLoader.getResource(fileName).getFile());

                    deploymentBuilder.addInputStream(fileName, new FileInputStream(file));

                    deploymentBuilder.name(fileName);
                    deploymentBuilder.tenantId("jhipster");

                    Deployment deployment = deploymentBuilder.deploy();

                } catch (Exception ex) {
                    if (ex instanceof ActivitiException) {
                        throw (ActivitiException)ex;
                    } else {
                        throw new ActivitiException(ex.getMessage(), ex);
                    }
                }

            }

        };


    }


    // TODO : test un deploy avec DeploymentCollectionResource


}
