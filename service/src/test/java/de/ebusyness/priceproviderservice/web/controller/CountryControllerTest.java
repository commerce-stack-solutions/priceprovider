package de.ebusyness.priceproviderservice.web.controller.adminapi;

import de.ebusyness.commons.exception.DataIntegrityException;
import de.ebusyness.commons.exception.EntityAlreadyExistsException;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.commons.web.rest.Message;
import de.ebusyness.priceproviderservice.web.controller.adminapi.CountryController;
import de.ebusyness.priceproviderservice.facade.country.CountryFacade;
import de.ebusyness.priceproviderservice.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.context.annotation.Import;

@Import(TestSecurityConfig.class)
@WebMvcTest(CountryController.class)
public class CountryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CountryFacade countryFacade;

    @Test
    public void testDelete() throws Exception {
        mockMvc.perform(delete("/admin/api/countries/DE"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteNotFound() throws Exception {
        doThrow(new IllegalArgumentException("Country with isoKey XX not found"))
                .when(countryFacade).delete("XX");

        mockMvc.perform(delete("/admin/api/countries/XX"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testBulkDeleteCountries() throws Exception {
        mockMvc.perform(post("/admin/api/countries/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"DE\", \"AT\"]"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testBulkDeleteCountriesWithReferencedCountry() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("ids", "DE");

        doThrow(new DataIntegrityException("common.errors.dataIntegrity.referenced", params))
                .when(countryFacade).bulkDeleteCountries(org.mockito.Mockito.anyList());

        mockMvc.perform(post("/admin/api/countries/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"DE\", \"AT\"]"))
                .andExpect(status().isConflict());
    }

    @Test
    public void testCreate_MissingIsoKey_Returns400() throws Exception {
        Message validationMessage = new Message(Message.MessageType.ERROR, "common.errors.validation.idRequired", Map.of("field", "isoKey"), List.of("isoKey"));
        when(countryFacade.create(any()))
                .thenThrow(new EntityValidationException("common.errors.validation.idRequired", validationMessage));

        mockMvc.perform(post("/admin/api/countries/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":{\"de\":\"Deutschland\",\"en\":\"Germany\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages").exists())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("common.errors.validation.idRequired"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("isoKey"));
    }

    @Test
    public void testCreate_CountryAlreadyExists_Returns409() throws Exception {
        when(countryFacade.create(any()))
                .thenThrow(new EntityAlreadyExistsException("common.errors.country.alreadyExists", Map.of("id", "DE"), List.of("isoKey")));

        mockMvc.perform(post("/admin/api/countries/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"isoKey\":\"DE\",\"name\":{\"de\":\"Deutschland\",\"en\":\"Germany\"}}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.$messages").exists())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("common.errors.country.alreadyExists"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("isoKey"));
    }

    @Test
    public void testPatch_ImmutableField_Returns400() throws Exception {
        de.ebusyness.commons.web.rest.ErrorResponse errorResponse = new de.ebusyness.commons.web.rest.ErrorResponse();
        errorResponse.addMessage(new Message(Message.MessageType.ERROR,
                "Field 'isoKey' cannot be changed", List.of("isoKey")));

        when(countryFacade.patch(anyString(), any()))
                .thenThrow(new DataMappingException("Validation failed", errorResponse));

        mockMvc.perform(patch("/admin/api/countries/DE")
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"replace\",\"path\":\"/isoKey\",\"value\":\"AT\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("Field 'isoKey' cannot be changed"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("isoKey"));
    }
}
