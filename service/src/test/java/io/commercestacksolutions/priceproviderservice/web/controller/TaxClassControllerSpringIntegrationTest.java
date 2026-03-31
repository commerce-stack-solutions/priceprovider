package io.commercestacksolutions.priceproviderservice.web.controller;

import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.query.messagekeys.MessageKeys;
import io.commercestacksolutions.priceproviderservice.facade.taxclass.TaxClassFacade;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.context.annotation.Import;
import io.commercestacksolutions.priceproviderservice.config.TestSecurityConfig;

@Import(TestSecurityConfig.class)
@SpringBootTest
@AutoConfigureMockMvc
public class TaxClassControllerSpringIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TaxClassFacade taxClassFacade;

    @Test
    public void whenInvalidTaxRate_thenBadRequestAndMessageKey() throws Exception {
        Map<String, String> params = Map.of(
                "field", "taxRate",
                "expectedType", "decimal",
                "actualValue", "not-a-number"
        );
        InvalidParameterException ex = new InvalidParameterException(MessageKeys.ERROR_QUERY_INVALID_VALUE_TYPE, params, List.of("taxRate"));
        doThrow(ex).when(taxClassFacade).getTaxClasses(org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());

        mockMvc.perform(get("/admin/api/taxclasses").param("q", "taxRate:not-a-number").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key").value(MessageKeys.ERROR_QUERY_INVALID_VALUE_TYPE));
    }
}
