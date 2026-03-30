package de.ebusyness.priceproviderservice.web.controller.adminapi;

import de.ebusyness.commons.exception.DataIntegrityException;
import de.ebusyness.commons.exception.EntityAlreadyExistsException;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.commons.web.rest.Message;
import de.ebusyness.priceproviderservice.web.controller.adminapi.CurrencyController;
import de.ebusyness.priceproviderservice.facade.currency.CurrencyFacade;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.context.annotation.Import;
import de.ebusyness.priceproviderservice.config.TestSecurityConfig;

@Import(TestSecurityConfig.class)
@WebMvcTest(CurrencyController.class)
public class CurrencyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CurrencyFacade currencyFacade;

    @Test
    public void testDelete() throws Exception {
        mockMvc.perform(delete("/admin/api/currencies/USD"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testDeleteNotFound() throws Exception {
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Currency with currencyKey XYZ not found"))
                .when(currencyFacade).delete("XYZ");

        mockMvc.perform(delete("/admin/api/currencies/XYZ"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void testBulkDeleteCurrencies() throws Exception {
        mockMvc.perform(post("/admin/api/currencies/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"USD\", \"EUR\", \"GBP\"]"))
                .andExpect(status().isNoContent());
    }

    @Test
    public void testBulkDeleteCurrenciesWithReferencedCurrency() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("ids", "EUR");

        org.mockito.Mockito.doThrow(new DataIntegrityException("common.errors.dataIntegrity.referenced", params))
                .when(currencyFacade).bulkDeleteCurrencies(org.mockito.Mockito.anyList());

        mockMvc.perform(post("/admin/api/currencies/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"USD\", \"EUR\", \"GBP\"]"))
                .andExpect(status().isConflict());
    }

    /**
     * Verifies that when a currency is referenced by a Country, the 409 response body
     * contains the expected message key and referencedBy / ids / referencedByIds parameters.
     */
    @Test
    public void testBulkDeleteCurrenciesReferencedByCountry_Returns409WithReferencedByMessage() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("ids", "GBP");
        params.put("referencedBy", "Country");
        params.put("referencedByIds", "GB");

        org.mockito.Mockito.doThrow(new DataIntegrityException(
                        "common.errors.dataIntegrity.referencedByEntity", params,
                        List.of("allowedCurrencyRefs", "primaryCurrencyRef")))
                .when(currencyFacade).bulkDeleteCurrencies(org.mockito.Mockito.anyList());

        mockMvc.perform(post("/admin/api/currencies/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"GBP\"]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.$messages").exists())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("common.errors.dataIntegrity.referencedByEntity"))
                .andExpect(jsonPath("$.$messages[0].parameters.ids").value("GBP"))
                .andExpect(jsonPath("$.$messages[0].parameters.referencedBy").value("Country"))
                .andExpect(jsonPath("$.$messages[0].parameters.referencedByIds").value("GB"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("allowedCurrencyRefs"))
                .andExpect(jsonPath("$.$messages[0].fields[1]").value("primaryCurrencyRef"));
    }

    /**
     * Verifies that bulk-deleting a mix of in-use and free currencies returns 409 with
     * only the in-use currency key in the ids parameter, including the referencing country IDs.
     */
    @Test
    public void testBulkDeleteCurrenciesPartiallyInUse_Returns409WithOnlyInUseCurrencyIds() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("ids", "EUR");
        params.put("referencedBy", "Country");
        params.put("referencedByIds", "DE, AT, CH, FI");

        org.mockito.Mockito.doThrow(new DataIntegrityException(
                        "common.errors.dataIntegrity.referencedByEntity", params,
                        List.of("allowedCurrencyRefs", "primaryCurrencyRef")))
                .when(currencyFacade).bulkDeleteCurrencies(org.mockito.Mockito.anyList());

        mockMvc.perform(post("/admin/api/currencies/bulk-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"CNY\", \"EUR\"]"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.$messages[0].parameters.ids").value("EUR"))
                .andExpect(jsonPath("$.$messages[0].parameters.referencedBy").value("Country"))
                .andExpect(jsonPath("$.$messages[0].parameters.referencedByIds").value("DE, AT, CH, FI"));
    }

    @Test
    public void testCreate_MissingCurrencyKey_Returns400() throws Exception {
        Message validationMessage = new Message(Message.MessageType.ERROR, "common.errors.validation.idRequired", Map.of("field", "currencyKey"), List.of("currencyKey"));
        when(currencyFacade.create(any()))
                .thenThrow(new EntityValidationException("common.errors.validation.idRequired", validationMessage));

        mockMvc.perform(post("/admin/api/currencies/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"symbol\":\"€\",\"name\":{\"de\":\"Euro\",\"en\":\"Euro\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages").exists())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("common.errors.validation.idRequired"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("currencyKey"));
    }

    @Test
    public void testCreate_CurrencyAlreadyExists_Returns409() throws Exception {
        when(currencyFacade.create(any()))
                .thenThrow(new EntityAlreadyExistsException("common.errors.currency.alreadyExists", Map.of("id", "EUR"), List.of("currencyKey")));

        mockMvc.perform(post("/admin/api/currencies/create")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currencyKey\":\"EUR\",\"symbol\":\"€\",\"name\":{\"de\":\"Euro\",\"en\":\"Euro\"}}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.$messages").exists())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("common.errors.currency.alreadyExists"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("currencyKey"));
    }
}

