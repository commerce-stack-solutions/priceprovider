package io.commercestacksolutions.priceproviderservice.web.controller.adminapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.commercestacksolutions.commons.exception.EntityAlreadyExistsException;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.web.controller.ExceptionHandlerAdvice;
import io.commercestacksolutions.priceproviderservice.web.controller.adminapi.LanguageController;
import io.commercestacksolutions.priceproviderservice.facade.language.LanguageFacade;
import io.commercestacksolutions.priceproviderservice.facade.language.restentity.LanguageRestEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.context.annotation.Import;
import io.commercestacksolutions.priceproviderservice.config.TestSecurityConfig;

@Import(TestSecurityConfig.class)
@WebMvcTest({LanguageController.class, ExceptionHandlerAdvice.class})
public class LanguageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LanguageFacade languageFacade;

    @Test
    public void testBulkDeleteLanguages() throws Exception {
        mockMvc.perform(post("/admin/api/languages/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"en\", \"de\", \"fr\"]"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testPatchLanguage_InactiveAndMandatory_Returns400() throws Exception {
        // Create error message from LanguageInactiveMandatoryRule
        Message errorMessage = new Message(
                Message.MessageType.ERROR,
                "common.errors.language.mandatoryMustBeActive",
                Arrays.asList("active", "mandatory")
        );

        // Mock the facade to throw EntityValidationException
        when(languageFacade.patch(anyString(), any()))
                .thenThrow(new EntityValidationException(
                        "Validation failed",
                        errorMessage
                ));

        // Perform PATCH request to set active=false (assuming language is mandatory)
        // This should return 400 BAD REQUEST, not 200 OK
        mockMvc.perform(patch("/admin/api/languages/en")
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"replace\",\"path\":\"/active\",\"value\":false}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("common.errors.language.mandatoryMustBeActive"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("active"))
                .andExpect(jsonPath("$.$messages[0].fields[1]").value("mandatory"));
    }
    
    @Test
    public void testPatchLanguage_InactiveAndMandatory_CorrectMessageKey() throws Exception {
        // Verifies that the specific error message for inactive+mandatory validation is returned
        Message errorMessage = new Message(
                Message.MessageType.ERROR,
                "common.errors.language.mandatoryMustBeActive",
                Arrays.asList("active", "mandatory")
        );

        when(languageFacade.patch(anyString(), any()))
                .thenThrow(new EntityValidationException(
                        "Validation failed",
                        errorMessage
                ));

        mockMvc.perform(patch("/admin/api/languages/en")
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"replace\",\"path\":\"/active\",\"value\":false}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("common.errors.language.mandatoryMustBeActive"));
    }

    @Test
    public void testPatchLanguage_ValidOperation_ReturnsUpdatedEntity() throws Exception {
        LanguageRestEntity updated = new LanguageRestEntity();
        updated.setIsoKey("en");
        updated.setActive(true);
        updated.setMandatory(true);

        when(languageFacade.patch(anyString(), any())).thenReturn(updated);

        mockMvc.perform(patch("/admin/api/languages/en")
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"replace\",\"path\":\"/active\",\"value\":true}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isoKey").value("en"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    public void testCreate_MissingIsoKey_Returns400() throws Exception {
        // Simulate EntityValidationException when isoKey is missing
        Message validationMessage = new Message(Message.MessageType.ERROR, "common.errors.validation.idRequired", Map.of("field", "isoKey"), Arrays.asList("isoKey"));
        when(languageFacade.create(any()))
                .thenThrow(new EntityValidationException("common.errors.validation.idRequired", validationMessage));

        mockMvc.perform(post("/admin/api/languages/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"active\":true,\"mandatory\":false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages").exists())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("common.errors.validation.idRequired"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("isoKey"));
    }

    @Test
    public void testCreate_LanguageAlreadyExists_Returns409() throws Exception {
        // Simulate EntityAlreadyExistsException when language already exists
        when(languageFacade.create(any()))
                .thenThrow(new EntityAlreadyExistsException("common.errors.language.alreadyExists", Map.of("isoKey", "en"), Arrays.asList("isoKey")));

        mockMvc.perform(post("/admin/api/languages/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isoKey\":\"en\",\"active\":true,\"mandatory\":false}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.$messages").exists())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("common.errors.language.alreadyExists"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("isoKey"));
    }
}
