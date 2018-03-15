package be.civadis.workflow.rest;

import be.civadis.workflow.security.AuthoritiesConstants;
import org.activiti.engine.ActivitiException;
import org.activiti.engine.ActivitiIllegalArgumentException;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.impl.DeploymentQueryProperty;
import org.activiti.engine.query.QueryProperty;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.repository.DeploymentQuery;
import org.activiti.rest.common.api.DataResponse;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.repository.DeploymentResponse;
import org.activiti.rest.service.api.repository.DeploymentsPaginateList;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipInputStream;

/**
 * Ressource permettant le déploiements de processus (peut être sécurisée par oauth2)
 * Rem :
 *  Les déploiements peuvent aussi être réalisés par l'UI workflow
 *  L'API rest native de workflow peut être exposée par Spring-boot, mais elle est sécurisée par basic oauth
 */
@RestController
@RequestMapping("/workflow")
public class CustomDeploymentResource {
    protected static final String DEPRECATED_API_DEPLOYMENT_SEGMENT = "deployment";
    private static Map<String, QueryProperty> allowedSortProperties = new HashMap();
    @Autowired
    protected RestResponseFactory restResponseFactory;
    @Autowired
    protected RepositoryService repositoryService;

    public CustomDeploymentResource() {
    }

    /**
     * Retourne la liste des déploiements
     * @param allRequestParams
     * @param request
     * @return
     */
    @GetMapping("/deployments")
    @Secured({AuthoritiesConstants.ADMIN})
    public DataResponse getDeployments(@RequestParam Map<String, String> allRequestParams, HttpServletRequest request) {
        DeploymentQuery deploymentQuery = this.repositoryService.createDeploymentQuery();
        if (allRequestParams.containsKey("name")) {
            deploymentQuery.deploymentName((String)allRequestParams.get("name"));
        }

        if (allRequestParams.containsKey("nameLike")) {
            deploymentQuery.deploymentNameLike((String)allRequestParams.get("nameLike"));
        }

        if (allRequestParams.containsKey("category")) {
            deploymentQuery.deploymentCategory((String)allRequestParams.get("category"));
        }

        if (allRequestParams.containsKey("categoryNotEquals")) {
            deploymentQuery.deploymentCategoryNotEquals((String)allRequestParams.get("categoryNotEquals"));
        }

        if (allRequestParams.containsKey("tenantId")) {
            deploymentQuery.deploymentTenantId((String)allRequestParams.get("tenantId"));
        }

        if (allRequestParams.containsKey("tenantIdLike")) {
            deploymentQuery.deploymentTenantIdLike((String)allRequestParams.get("tenantIdLike"));
        }

        if (allRequestParams.containsKey("withoutTenantId")) {
            Boolean withoutTenantId = Boolean.valueOf((String)allRequestParams.get("withoutTenantId"));
            if (withoutTenantId.booleanValue()) {
                deploymentQuery.deploymentWithoutTenantId();
            }
        }

        DataResponse response = (new DeploymentsPaginateList(this.restResponseFactory)).paginateList(allRequestParams, deploymentQuery, "id", allowedSortProperties);
        return response;
    }

    /**
     * Déploie une définition de processus
     * @param tenantId
     * @param request
     * @param response
     * @return
     */
    @PostMapping("/deployments")
    @Secured({AuthoritiesConstants.ADMIN})
    public DeploymentResponse uploadDeployment(@RequestParam(value = "tenantId",required = false) String tenantId, HttpServletRequest request, HttpServletResponse response) {
        if (!(request instanceof MultipartHttpServletRequest)) {
            throw new ActivitiIllegalArgumentException("Multipart request is required");
        } else {
            MultipartHttpServletRequest multipartRequest = (MultipartHttpServletRequest)request;
            if (multipartRequest.getFileMap().size() == 0) {
                throw new ActivitiIllegalArgumentException("Multipart request with file content is required");
            } else {
                MultipartFile file = (MultipartFile)multipartRequest.getFileMap().values().iterator().next();

                try {
                    DeploymentBuilder deploymentBuilder = this.repositoryService.createDeployment();
                    String fileName = file.getOriginalFilename();
                    if (StringUtils.isEmpty(fileName) || !fileName.endsWith(".bpmn20.xml") && !fileName.endsWith(".bpmn") && !fileName.toLowerCase().endsWith(".bar") && !fileName.toLowerCase().endsWith(".zip")) {
                        fileName = file.getName();
                    }

                    if (!fileName.endsWith(".bpmn20.xml") && !fileName.endsWith(".bpmn")) {
                        if (!fileName.toLowerCase().endsWith(".bar") && !fileName.toLowerCase().endsWith(".zip")) {
                            throw new ActivitiIllegalArgumentException("File must be of type .bpmn20.xml, .bpmn, .bar or .zip");
                        }

                        deploymentBuilder.addZipInputStream(new ZipInputStream(file.getInputStream()));
                    } else {
                        deploymentBuilder.addInputStream(fileName, file.getInputStream());
                    }

                    deploymentBuilder.name(fileName);
                    if (tenantId != null) {
                        deploymentBuilder.tenantId(tenantId);
                    }

                    Deployment deployment = deploymentBuilder.deploy();
                    response.setStatus(HttpStatus.CREATED.value());
                    return this.restResponseFactory.createDeploymentResponse(deployment);
                } catch (Exception var9) {
                    if (var9 instanceof ActivitiException) {
                        throw (ActivitiException)var9;
                    } else {
                        throw new ActivitiException(var9.getMessage(), var9);
                    }
                }
            }
        }
    }

    static {
        allowedSortProperties.put("id", DeploymentQueryProperty.DEPLOYMENT_ID);
        allowedSortProperties.put("name", DeploymentQueryProperty.DEPLOYMENT_NAME);
        allowedSortProperties.put("deployTime", DeploymentQueryProperty.DEPLOY_TIME);
        allowedSortProperties.put("tenantId", DeploymentQueryProperty.DEPLOYMENT_TENANT_ID);
    }
}
