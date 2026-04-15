package io.commercestacksolutions.priceproviderservice.web.controller.adminapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.commercestacksolutions.commons.exception.NotFoundException;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.commons.web.rest.ErrorResponse;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.web.controller.ExceptionHandlerAdvice;
import io.commercestacksolutions.priceproviderservice.web.controller.adminapi.CurrencyController;
import io.commercestacksolutions.priceproviderservice.web.controller.adminapi.PriceRowController;
import io.commercestacksolutions.priceproviderservice.web.controller.adminapi.UnitController;
import io.commercestacksolutions.priceproviderservice.facade.currency.CurrencyFacade;
import io.commercestacksolutions.priceproviderservice.facade.currency.restentity.CurrencyRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.pricerow.PriceRowFacade;
import io.commercestacksolutions.priceproviderservice.facade.pricerow.restentity.PriceRowRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.unit.UnitFacadeService;
import io.commercestacksolutions.priceproviderservice.facade.unit.restentity.UnitRestEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import io.commercestacksolutions.priceproviderservice.config.TestSecurityConfig;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for validation framework covering:
 * - Controller exception handling
 * - Facade validation with PatchValidator
 * - ExceptionHandlerAdvice error responses
 */
@Import(TestSecurityConfig.class)
@WebMvcTest({PriceRowController.class, CurrencyController.class, UnitController.class, ExceptionHandlerAdvice.class})
public class ValidationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PriceRowFacade priceRowFacade;

    @MockBean
    private CurrencyFacade currencyFacade;

    @MockBean
    private UnitFacadeService unitFacade;

    // ========== NotFoundException Tests ==========

    @Test
    public void testGetNonExistingPriceRow_Returns404() throws Exception {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.addMessage(new Message(Message.MessageType.ERROR, "PriceRow with id '999' not found"));
        
        when(priceRowFacade.getPriceRow(anyString(), any()))
                .thenThrow(new NotFoundException("PriceRow with id '999' not found", errorResponse));

        mockMvc.perform(get("/admin/api/pricerows/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("PriceRow with id '999' not found"));
    }

    @Test
    public void testGetNonExistingCurrency_Returns404() throws Exception {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.addMessage(new Message(Message.MessageType.ERROR, "Currency with currencyKey 'XXX' not found"));
        
        when(currencyFacade.getCurrency(anyString(), any()))
                .thenThrow(new NotFoundException("Currency with currencyKey 'XXX' not found", errorResponse));

        mockMvc.perform(get("/admin/api/currencies/XXX"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("Currency with currencyKey 'XXX' not found"));
    }

    @Test
    public void testGetNonExistingUnit_Returns404() throws Exception {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.addMessage(new Message(Message.MessageType.ERROR, "Unit with symbol 'xyz' not found"));
        
        when(unitFacade.getUnit(anyString(), any()))
                .thenThrow(new NotFoundException("Unit with symbol 'xyz' not found", errorResponse));

        mockMvc.perform(get("/admin/api/units/xyz"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("Unit with symbol 'xyz' not found"));
    }

    // ========== DataMappingException Tests (Invalid PATCH operations) ==========

    @Test
    public void testPatchPriceRow_RemoveMandatoryField_Returns400() throws Exception {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.addMessage(new Message(Message.MessageType.ERROR, 
                "Field 'priceValue' is mandatory and cannot be removed", List.of("priceValue")));
        
        when(priceRowFacade.patch(anyString(), any()))
                .thenThrow(new DataMappingException("Validation failed", errorResponse));

        mockMvc.perform(patch("/admin/api/pricerows/1")
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"remove\",\"path\":\"/priceValue\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("Field 'priceValue' is mandatory and cannot be removed"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("priceValue"));
    }

    @Test
    public void testPatchPriceRow_ChangeImmutableField_Returns400() throws Exception {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.addMessage(new Message(Message.MessageType.ERROR, 
                "Field 'id' cannot be changed", List.of("id")));
        
        when(priceRowFacade.patch(anyString(), any()))
                .thenThrow(new DataMappingException("Validation failed", errorResponse));

        mockMvc.perform(patch("/admin/api/pricerows/1")
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"replace\",\"path\":\"/id\",\"value\":\"999\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("Field 'id' cannot be changed"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("id"));
    }

    @Test
    public void testPatchCurrency_ChangeImmutableCurrencyKey_Returns400() throws Exception {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.addMessage(new Message(Message.MessageType.ERROR, 
                "Field 'currencyKey' cannot be changed", List.of("currencyKey")));
        
        when(currencyFacade.patch(anyString(), any()))
                .thenThrow(new DataMappingException("Validation failed", errorResponse));

        mockMvc.perform(patch("/admin/api/currencies/USD")
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"replace\",\"path\":\"/currencyKey\",\"value\":\"EUR\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("Field 'currencyKey' cannot be changed"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("currencyKey"));
    }

    @Test
    public void testPatchUnit_ChangeImmutableSymbol_Returns400() throws Exception {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.addMessage(new Message(Message.MessageType.ERROR, 
                "Field 'symbol' cannot be changed", List.of("symbol")));
        
        when(unitFacade.patch(anyString(), any()))
                .thenThrow(new DataMappingException("Validation failed", errorResponse));

        mockMvc.perform(patch("/admin/api/units/kg")
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"replace\",\"path\":\"/symbol\",\"value\":\"g\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("Field 'symbol' cannot be changed"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("symbol"));
    }

    // ========== JSON Deserialization Error Tests ==========

    @Test
    public void testPatchPriceRow_InvalidBooleanValue_Returns400() throws Exception {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.addMessage(new Message(Message.MessageType.ERROR, 
                "Invalid value for field 'taxIncluded': expected true or false", List.of("taxIncluded")));
        
        when(priceRowFacade.patch(anyString(), any()))
                .thenThrow(new DataMappingException("Validation failed", errorResponse));

        mockMvc.perform(patch("/admin/api/pricerows/1")
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"replace\",\"path\":\"/taxIncluded\",\"value\":\"yes\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("Invalid value for field 'taxIncluded': expected true or false"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("taxIncluded"));
    }

    @Test
    public void testPatchPriceRow_NonExistingUnitReference_Returns400() throws Exception {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.addMessage(new Message(Message.MessageType.ERROR, 
                "Unit with symbol 'xyz' not found", List.of("unitRef")));
        
        when(priceRowFacade.patch(anyString(), any()))
                .thenThrow(new DataMappingException("Validation failed", errorResponse));

        mockMvc.perform(patch("/admin/api/pricerows/1")
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"replace\",\"path\":\"/unitRef\",\"value\":\"xyz\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("Unit with symbol 'xyz' not found"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("unitRef"));
    }

    @Test
    public void testPatchPriceRow_NonExistingCurrencyReference_Returns400() throws Exception {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.addMessage(new Message(Message.MessageType.ERROR, 
                "Currency with currencyKey 'USDD' not found", List.of("currencyRef")));
        
        when(priceRowFacade.patch(anyString(), any()))
                .thenThrow(new DataMappingException("Validation failed", errorResponse));

        mockMvc.perform(patch("/admin/api/pricerows/1")
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"replace\",\"path\":\"/currencyRef\",\"value\":\"USDD\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("Currency with currencyKey 'USDD' not found"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("currencyRef"));
    }

    @Test
    public void testPatchUnit_NonExistingBaseUnitReference_Returns400() throws Exception {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.addMessage(new Message(Message.MessageType.ERROR, 
                "Base unit with symbol 'y' not found", List.of("baseUnitRef")));
        
        when(unitFacade.patch(anyString(), any()))
                .thenThrow(new DataMappingException("Validation failed", errorResponse));

        mockMvc.perform(patch("/admin/api/units/kg")
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"replace\",\"path\":\"/baseUnitRef\",\"value\":\"y\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("Base unit with symbol 'y' not found"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("baseUnitRef"));
    }

    // ========== LocalizedFieldValidationRule Tests ==========

    @Test
    public void testPatchCurrency_MissingMandatoryLanguage_Returns400() throws Exception {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.addMessage(new Message(Message.MessageType.ERROR, 
                "Missing mandatory language values for field 'name': [en]", List.of("name")));
        
        when(currencyFacade.patch(anyString(), any()))
                .thenThrow(new DataMappingException("Validation failed", errorResponse));

        mockMvc.perform(patch("/admin/api/currencies/USD")
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"remove\",\"path\":\"/name/en\"}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("Missing mandatory language values for field 'name': [en]"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("name"));
    }

    @Test
    public void testPatchUnit_MissingMandatoryLanguage_Returns400() throws Exception {
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.addMessage(new Message(Message.MessageType.ERROR, 
                "Missing mandatory language values for field 'name': [de, en]", List.of("name")));
        
        when(unitFacade.patch(anyString(), any()))
                .thenThrow(new DataMappingException("Validation failed", errorResponse));

        mockMvc.perform(patch("/admin/api/units/kg")
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"replace\",\"path\":\"/name\",\"value\":{\"fr\":\"kilogramme\"}}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].type").value("ERROR"))
                .andExpect(jsonPath("$.$messages[0]['message-key']").value("Missing mandatory language values for field 'name': [de, en]"))
                .andExpect(jsonPath("$.$messages[0].fields[0]").value("name"));
    }

    // ========== Successful PATCH Operations ==========

    @Test
    public void testPatchPriceRow_ValidOperation_ReturnsUpdatedEntity() throws Exception {
        PriceRowRestEntity updated = new PriceRowRestEntity();
        updated.setPricedResourceId("updated-resource");
        
        when(priceRowFacade.patch(anyString(), any())).thenReturn(updated);

        mockMvc.perform(patch("/admin/api/pricerows/1")
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"replace\",\"path\":\"/pricedResourceId\",\"value\":\"updated-resource\"}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pricedResourceId").value("updated-resource"));
    }

    @Test
    public void testPatchCurrency_ValidOperation_ReturnsUpdatedEntity() throws Exception {
        CurrencyRestEntity updated = new CurrencyRestEntity();
        updated.setSymbol("$");
        
        when(currencyFacade.patch(anyString(), any())).thenReturn(updated);

        mockMvc.perform(patch("/admin/api/currencies/USD")
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"replace\",\"path\":\"/symbol\",\"value\":\"$\"}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("$"));
    }

    @Test
    public void testPatchUnit_ValidOperation_ReturnsUpdatedEntity() throws Exception {
        UnitRestEntity updated = new UnitRestEntity();
        updated.setFactor(new java.math.BigDecimal("1000.0"));
        
        when(unitFacade.patch(anyString(), any())).thenReturn(updated);

        mockMvc.perform(patch("/admin/api/units/kg")
                        .contentType("application/json-patch+json")
                        .content("[{\"op\":\"replace\",\"path\":\"/factor\",\"value\":1000.0}]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.factor").value(1000.0));
    }
}
