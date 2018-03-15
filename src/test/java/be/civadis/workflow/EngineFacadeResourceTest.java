package be.civadis.workflow;


import be.civadis.workflow.activiti.EngineFacade;
import be.civadis.workflow.model.Applicant;
import be.civadis.workflow.rest.EngineFacadeResource;
import be.civadis.workflow.security.AuthoritiesConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.activiti.rest.service.api.RestResponseFactory;
import org.activiti.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.security.test.context.support.WithMockUser;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MyApp.class)
public class EngineFacadeResourceTest {

    private static final Instant DEFAULT_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    @Autowired
    private EngineFacade engineFacade;

    @Autowired
    private RestResponseFactory restResponseFactory;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private EntityManager em;

    private MockMvc restMockMvc;

    private ObjectMapper mapper;


    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final EngineFacadeResource engineFacadeResource = new EngineFacadeResource(engineFacade, restResponseFactory);
        this.restMockMvc = MockMvcBuilders.standaloneSetup(engineFacadeResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            //.setControllerAdvice(exceptionTranslator)
            //.setConversionService(createFormattingConversionService())
            .setMessageConverters(jacksonMessageConverter).build();
        mapper = new ObjectMapper();
    }

    @Before
    public void initTest() {

    }

    @Test
    @Transactional
    @WithMockUser(username="admin", authorities ={AuthoritiesConstants.ADMIN,AuthoritiesConstants.USER})
    public void testProcessByAdmin() throws Exception {

        //pour l'admin, on ne passe pas de user ou groupes au query, il peut tout voir

        //List<String> tenants = applicationProperties.getSchemas();
        //TenantContext.setCurrentTenant(tenants.get(0));

        Applicant applicant = new Applicant();
        applicant.setName("John Doe");
        applicant.setEmail("john.doe@alfresco.com");
        applicant.setPhoneNumber("123456789");

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("applicant", applicant);

        // start process
        MvcResult resPr = restMockMvc.perform(
            post("/workflow/processes/hireProcessWithJpa/start")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(mapper.writeValueAsString(variables))
        ).andExpect(status().isOk()).andReturn();
        ProcessInstanceResponse processInstance = mapper.readValue(resPr.getResponse().getContentAsString(), ProcessInstanceResponse.class);

        // First, the 'phone interview' should be active
        MvcResult res = restMockMvc.perform(
            get("/workflow/tasks-claimable")
                .param("user", "admin")
                .param("groups", "dev-managers")
                .param("processInstanceId", processInstance.getId())
        ).andExpect(status().isOk()).andReturn();
        System.out.println(res.getResponse().getContentAsString());
        List<Map> tasks = mapper.readValue(res.getResponse().getContentAsString(), new TypeReference<List<Map>>(){});
        Assert.assertEquals("Telephone interview", tasks.get(0).get("name"));

        // claim task
        restMockMvc.perform(
            post("/workflow/tasks/"+ tasks.get(0).get("id") + "/claim")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .param("userId", "admin")
        ).andExpect(status().isOk());

        // Check assigned task
        MvcResult res2 = restMockMvc.perform(
            get("/workflow/tasks-assigned")
                .param("user", "admin")
                .param("processInstanceId", processInstance.getId())
        ).andExpect(status().isOk()).andReturn();
        List<Map> taskAssignedList = mapper.readValue(res2.getResponse().getContentAsString(), new TypeReference<List<Map>>(){});
        Assert.assertEquals("Telephone interview", taskAssignedList.get(0).get("name"));

        // Completing the phone interview with success should trigger two new tasks
        Map<String, Object> taskVariables = new HashMap<String, Object>();
        taskVariables.put("telephoneInterviewOutcome", true);
        restMockMvc.perform(
            post("/workflow/tasks/"+ taskAssignedList.get(0).get("id") + "/complete")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(mapper.writeValueAsString(taskVariables))
        ).andExpect(status().isOk());

        // check assigned tasks
        MvcResult res3 = restMockMvc.perform(
            get("/workflow/tasks-claimable")
                .param("processInstanceId", processInstance.getId())
                .param("sort", "name,asc")
        ).andExpect(status().isOk()).andReturn();
        List<Map> taskAssignedList2 = mapper.readValue(res3.getResponse().getContentAsString(), new TypeReference<List<Map>>(){});
        Assert.assertEquals(2, taskAssignedList2.size());
        Assert.assertEquals("Financial negotiation", taskAssignedList2.get(0).get("name"));
        Assert.assertEquals("Tech interview", taskAssignedList2.get(1).get("name"));

    }

    @Test
    @Transactional
    @WithMockUser(username="phw", authorities ={AuthoritiesConstants.USER, "dev-managers", "dev-management", "finance"})
    public void testProcessByUser() throws Exception {

        //pour un user, on passe le user et ses roles aux queries

        //List<String> tenants = applicationProperties.getSchemas();
        //TenantContext.setCurrentTenant(tenants.get(0));

        Applicant applicant = new Applicant();
        applicant.setName("John Doe");
        applicant.setEmail("john.doe@alfresco.com");
        applicant.setPhoneNumber("123456789");

        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("applicant", applicant);

        // start process
        MvcResult resPr = restMockMvc.perform(
            post("/workflow/processes/hireProcessWithJpa/start")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(mapper.writeValueAsString(variables))
        ).andExpect(status().isOk()).andReturn();
        ProcessInstanceResponse processInstance = mapper.readValue(resPr.getResponse().getContentAsString(), ProcessInstanceResponse.class);

        // First, the 'phone interview' should be active
        MvcResult res = restMockMvc.perform(
            get("/workflow/my-tasks-claimable")
                .param("processInstanceId", processInstance.getId())
        ).andExpect(status().isOk()).andReturn();
        List<Map> tasks = mapper.readValue(res.getResponse().getContentAsString(), new TypeReference<List<Map>>(){});
        Assert.assertEquals("Telephone interview", tasks.get(0).get("name"));

        // claim task
        restMockMvc.perform(
            post("/workflow/my-tasks/"+ tasks.get(0).get("id") + "/claim")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
        ).andExpect(status().isOk());

        // Check assigned task
        MvcResult res2 = restMockMvc.perform(
            get("/workflow/my-tasks-assigned")
                .param("processInstanceId", processInstance.getId())
        ).andExpect(status().isOk()).andReturn();
        List<Map> taskAssignedList = mapper.readValue(res2.getResponse().getContentAsString(), new TypeReference<List<Map>>(){});
        Assert.assertEquals("Telephone interview", taskAssignedList.get(0).get("name"));

        // Completing the phone interview with success should trigger two new tasks
        Map<String, Object> taskVariables = new HashMap<String, Object>();
        taskVariables.put("telephoneInterviewOutcome", true);
        restMockMvc.perform(
            post("/workflow/my-tasks/"+ taskAssignedList.get(0).get("id") + "/complete")
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .content(mapper.writeValueAsString(taskVariables))
        ).andExpect(status().isOk());

        // check assigned tasks
        MvcResult res3 = restMockMvc.perform(
            get("/workflow/my-tasks-claimable")
                .param("processInstanceId", processInstance.getId())
                .param("sort", "name,asc")
        ).andExpect(status().isOk()).andReturn();
        List<Map> taskAssignedList2 = mapper.readValue(res3.getResponse().getContentAsString(), new TypeReference<List<Map>>(){});
        Assert.assertEquals(2, taskAssignedList2.size());
        Assert.assertEquals("Financial negotiation", taskAssignedList2.get(0).get("name"));
        Assert.assertEquals("Tech interview", taskAssignedList2.get(1).get("name"));

    }

}
