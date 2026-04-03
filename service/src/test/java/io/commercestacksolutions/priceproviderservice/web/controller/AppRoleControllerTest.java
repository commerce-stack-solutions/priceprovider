package io.commercestacksolutions.priceproviderservice.web.controller;

import io.commercestacksolutions.commons.exception.EntityAlreadyExistsException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.config.TestSecurityConfig;
import io.commercestacksolutions.priceproviderservice.facade.approle.AppRoleFacade;
import io.commercestacksolutions.priceproviderservice.web.controller.adminapi.AppRoleController;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AppRoleController.class)
@Import(TestSecurityConfig.class)
public class AppRoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AppRoleFacade appRoleFacade;

    @Test
    public void testBulkDeleteAppRoles_Success() throws Exception {
        mockMvc.perform(post("/admin/api/app-roles/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[1, 2]"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testCreate_MissingName_Returns400() throws Exception {
        Message validationMessage = new Message(Message.MessageType.ERROR, "common.errors.validation.idRequired", Map.of("field", "name"), List.of("name"));
        when(appRoleFacade.create(any()))
                .thenThrow(new EntityValidationException("common.errors.validation.idRequired", validationMessage));

        mockMvc.perform(post("/admin/api/app-roles/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Test Role\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages").exists())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("common.errors.validation.idRequired"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("name"));
    }

    @Test
    public void testCreate_AlreadyExists_Returns409() throws Exception {
        when(appRoleFacade.create(any()))
                .thenThrow(new EntityAlreadyExistsException("common.errors.appRole.alreadyExists", Map.of("name", "priceprovider.admin:Admin"), List.of("name")));

        mockMvc.perform(post("/admin/api/app-roles/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"priceprovider.admin:Admin\",\"description\":\"Full admin access\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.$messages").exists())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("common.errors.appRole.alreadyExists"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("name"));
    }
}


