package io.commercestacksolutions.priceproviderservice.web.controller.adminapi;

import io.commercestacksolutions.commons.exception.DataIntegrityException;
import io.commercestacksolutions.commons.exception.EntityAlreadyExistsException;
import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.query.QueryFilterRuntimeException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.facade.group.restentity.GroupRestEntity;
import io.commercestacksolutions.priceproviderservice.web.controller.adminapi.GroupController;
import io.commercestacksolutions.priceproviderservice.facade.group.GroupFacade;
import io.commercestacksolutions.priceproviderservice.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GroupController.class)
@Import(TestSecurityConfig.class)
public class GroupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GroupFacade groupFacade;

    @Test
    public void testBulkDeleteGroups_Success() throws Exception {
        mockMvc.perform(post("/admin/api/groups/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"GRP-001\", \"GRP-002\"]"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testBulkDeleteGroups_DataIntegrityViolation() throws Exception {
        // Simulate a data integrity violation when trying to delete a group
        doThrow(new DataIntegrityException("Cannot delete group 'ORG-CITY-COUNCIL' - it is referenced by other entities", 
                List.of("id")))
                .when(groupFacade).bulkDeleteGroups(any());

        mockMvc.perform(post("/admin/api/groups/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"ORG-CITY-COUNCIL\"]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.$messages").exists())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("Cannot delete group 'ORG-CITY-COUNCIL' - it is referenced by other entities"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("id"));
    }

    @Test
    public void testGetGroups_InvalidQueryField_Returns400() throws Exception {
        // Simulate a QueryFilterRuntimeException when an invalid field is used in the query
        // The controller catches this and unwraps it to InvalidParameterException
        when(groupFacade.getGroups(anyInt(), anyInt(), any(), any(), any(), eq("subRefs.invalidField:value")))
                .thenThrow(new QueryFilterRuntimeException(new InvalidParameterException("common.errors.query.fieldInvalid", List.of("subRefs.invalidField"))));

        mockMvc.perform(get("/admin/api/groups")
                        .param("q", "subRefs.invalidField:value"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages").exists())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("common.errors.query.fieldInvalid"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("subRefs.invalidField"));
    }

    @ParameterizedTest
    @CsvSource({
        "subRefs:>5, subRefs, >",
        "parentRefs:<2, parentRefs, <",
        "subRefs:[1 TO 5], subRefs, '[min TO max]'",
        "parentRefs:>=1, parentRefs, >=",
        "subRefs:<=10, subRefs, <="
    })
    public void testGetGroups_InvalidCollectionOperators_Return400(String query, String field, String operator) throws Exception {
        // Simulate a QueryFilterRuntimeException when a comparison/range operator is used on a collection field
        String errorField = field + " (operator: " + operator + ", valid: .exists:true or .exists:false)";
        when(groupFacade.getGroups(anyInt(), anyInt(), any(), any(), any(), eq(query)))
                .thenThrow(new QueryFilterRuntimeException(new InvalidParameterException(
                        "common.errors.query.invalidCollectionOperator", 
                        List.of(errorField))));

        mockMvc.perform(get("/admin/api/groups")
                        .param("q", query))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages").exists())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("common.errors.query.invalidCollectionOperator"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value(errorField));
    }

    @Test
    public void testCreate_MissingPath_Returns400() throws Exception {
        // Simulate EntityValidationException when ID is missing
        Message validationMessage = new Message(Message.MessageType.ERROR, "common.errors.validation.pathRequired", Map.of("field", "path"), List.of("path"));
        when(groupFacade.create(any()))
                .thenThrow(new EntityValidationException("common.errors.validation.pathRequired", validationMessage));

        mockMvc.perform(post("/admin/api/groups/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Test Group\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages").exists())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("common.errors.validation.pathRequired"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("path"));
    }

    @Test
    public void testCreate_GroupAlreadyExists_Returns409() throws Exception {
        // Simulate EntityAlreadyExistsException when group already exists
        when(groupFacade.create(any()))
                .thenThrow(new EntityAlreadyExistsException("common.errors.group.alreadyExists", Map.of("path", "GRP-001"), List.of("path")));

        mockMvc.perform(post("/admin/api/groups/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"path\":\"GRP-001\",\"name\":\"Test Group\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.$messages").exists())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("common.errors.group.alreadyExists"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("path"));
    }

    @Test
    public void testGetGroupByPath_Success() throws Exception {
        GroupRestEntity group = new GroupRestEntity();
        group.setPath("GRP-SALE-PROMO");
        when(groupFacade.getGroupByPath(eq("GRP-SALE-PROMO"), any())).thenReturn(group);

        mockMvc.perform(get("/admin/api/groups/by-path/GRP-SALE-PROMO"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("GRP-SALE-PROMO"));
    }

    @Test
    public void testGetGroupByPath_NotFound() throws Exception {
        when(groupFacade.getGroupByPath(eq("NONEXISTENT"), any()))
                .thenThrow(new NotFoundException(
                        "common.errors.group.notFound", Map.of("id", "NONEXISTENT")));

        mockMvc.perform(get("/admin/api/groups/by-path/NONEXISTENT"))
                .andExpect(status().isNotFound());
    }
}
