package de.ebusyness.priceproviderservice.web.controller;

import de.ebusyness.commons.exception.DataIntegrityException;
import de.ebusyness.commons.exception.EntityAlreadyExistsException;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.commons.web.rest.Message;
import de.ebusyness.priceproviderservice.config.TestSecurityConfig;
import de.ebusyness.priceproviderservice.facade.approle.AppPermissionFacade;
import de.ebusyness.priceproviderservice.web.controller.adminapi.AppPermissionController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
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

@WebMvcTest(AppPermissionController.class)
@Import(TestSecurityConfig.class)
public class AppPermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppPermissionFacade appPermissionFacade;

    @Test
    public void testBulkDeleteAppPermissions_Success() throws Exception {
        mockMvc.perform(post("/admin/api/app-permissions/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"priceprovider.admin:Group:read\", \"priceprovider.admin:Group:write\"]"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testBulkDeleteAppPermissions_DataIntegrityViolation() throws Exception {
        doThrow(new DataIntegrityException("Cannot delete app permission - it is referenced by other entities",
                List.of("id")))
                .when(appPermissionFacade).bulkDeleteAppPermissions(any());

        mockMvc.perform(post("/admin/api/app-permissions/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"priceprovider.admin:Group:read\"]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.$messages").exists())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("Cannot delete app permission - it is referenced by other entities"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("id"));
    }

    @Test
    public void testCreate_MissingId_Returns400() throws Exception {
        Message validationMessage = new Message(Message.MessageType.ERROR, "common.errors.validation.idRequired", Map.of("field", "id"), List.of("id"));
        when(appPermissionFacade.create(any()))
                .thenThrow(new EntityValidationException("common.errors.validation.idRequired", validationMessage));

        mockMvc.perform(post("/admin/api/app-permissions/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Test Permission\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages").exists())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("common.errors.validation.idRequired"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("id"));
    }

    @Test
    public void testCreate_AlreadyExists_Returns409() throws Exception {
        when(appPermissionFacade.create(any()))
                .thenThrow(new EntityAlreadyExistsException("common.errors.appPermission.alreadyExists", Map.of("id", "priceprovider.admin:Group:read"), List.of("id")));

        mockMvc.perform(post("/admin/api/app-permissions/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"priceprovider.admin:Group:read\",\"description\":\"Read groups\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.$messages").exists())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("common.errors.appPermission.alreadyExists"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("id"));
    }
}



