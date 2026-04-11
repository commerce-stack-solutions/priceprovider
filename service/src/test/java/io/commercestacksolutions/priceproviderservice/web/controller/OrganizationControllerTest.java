package io.commercestacksolutions.priceproviderservice.web.controller.adminapi;

import io.commercestacksolutions.commons.exception.DataIntegrityException;
import io.commercestacksolutions.commons.exception.EntityAlreadyExistsException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.web.controller.adminapi.OrganizationController;
import io.commercestacksolutions.priceproviderservice.facade.organization.OrganizationFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.context.annotation.Import;
import io.commercestacksolutions.priceproviderservice.config.TestSecurityConfig;

@Import(TestSecurityConfig.class)
@WebMvcTest(OrganizationController.class)
public class OrganizationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrganizationFacade organizationFacade;

    @Test
    public void testBulkDeleteOrganizations_Success() throws Exception {
        mockMvc.perform(post("/admin/api/organizations/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"ORG-001\", \"ORG-002\"]"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testBulkDeleteOrganizations_DataIntegrityViolation() throws Exception {
        // Simulate a data integrity violation when trying to delete an organization
        doThrow(new DataIntegrityException("Cannot delete organization 'ORG-MY-COMPANY' - it is referenced by other entities", 
                List.of("id")))
                .when(organizationFacade).bulkDeleteOrganizations(any());

        mockMvc.perform(post("/admin/api/organizations/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"ORG-MY-COMPANY\"]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.$messages").exists())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("Cannot delete organization 'ORG-MY-COMPANY' - it is referenced by other entities"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("id"));
    }

    @Test
    public void testCreate_MissingPath_Returns400() throws Exception {
        // Simulate EntityValidationException when path is missing
        Message validationMessage = new Message(Message.MessageType.ERROR, "common.errors.validation.pathRequired", Map.of("field", "path"), List.of("path"));
        when(organizationFacade.create(any()))
                .thenThrow(new EntityValidationException("common.errors.validation.pathRequired", validationMessage));

        mockMvc.perform(post("/admin/api/organizations/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Organization\",\"organizationType\":\"COMPANY\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages").exists())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("common.errors.validation.pathRequired"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("path"));
    }

    @Test
    public void testCreate_OrganizationAlreadyExists_Returns409() throws Exception {
        // Simulate EntityAlreadyExistsException when organization already exists
        when(organizationFacade.create(any()))
                .thenThrow(new EntityAlreadyExistsException("common.errors.organization.alreadyExists", Map.of("path", "ORG-001"), List.of("path")));

        mockMvc.perform(post("/admin/api/organizations/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"path\":\"ORG-001\",\"name\":\"Test Organization\",\"organizationType\":\"COMPANY\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.$messages").exists())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("common.errors.organization.alreadyExists"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("path"));
    }
}
