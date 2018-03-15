package be.civadis.workflow.activiti;

import be.civadis.workflow.security.TenantUtils;
import org.activiti.engine.IdentityService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.identity.Group;
import org.activiti.engine.identity.User;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.spring.SpringAsyncExecutor;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.activiti.spring.boot.AbstractProcessEngineAutoConfiguration;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

//désactivé dans ce projet car on travail avec un seul datasource pour la DB H2
//@Configuration
public class ActivitiConfiguration extends AbstractProcessEngineAutoConfiguration {

    /**
     * Definition du datasource pour workflow
     * @return
     */
    @Bean(name = "activitiDataSource")
    public DataSource activitiDataSource(DataSourceProperties dataSourceProperties){
        return DataSourceBuilder
                .create()
                .url(dataSourceProperties.getUrl())
                .username(dataSourceProperties .getUsername())
                .password(dataSourceProperties .getPassword())
                .type(dataSourceProperties .getType())
                .build();
    }

    /**
     * Indiquer a workflow le datasource à utiliser
     * @param transactionManager
     * @param springAsyncExecutor
     * @return
     * @throws IOException
     */
    @Bean
    public SpringProcessEngineConfiguration springProcessEngineConfiguration(
        @Qualifier(value = "activitiDataSource") DataSource activitiDataSource,
        PlatformTransactionManager transactionManager,
        SpringAsyncExecutor springAsyncExecutor) throws IOException {

        return baseSpringProcessEngineConfiguration(
            activitiDataSource,
            transactionManager,
            springAsyncExecutor);
    }

    /**
     * Définir un datasource général pour le service, afin de ne pas utilisé celui spécifique à workflow
     * @param dataSourceProperties
     * @return
     */
    @Bean(name = "mainDataSource")
    @Primary
    public DataSource dataSource(DataSourceProperties dataSourceProperties ){
        return DataSourceBuilder
            .create()
            .url(dataSourceProperties.getUrl())
            .username(dataSourceProperties .getUsername())
            .password(dataSourceProperties .getPassword())
            .type(dataSourceProperties .getType())
            .build();
    }

}
