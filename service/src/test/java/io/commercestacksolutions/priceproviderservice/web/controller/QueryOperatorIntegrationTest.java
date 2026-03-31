package io.commercestacksolutions.priceproviderservice.web.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.notNullValue;
import org.springframework.context.annotation.Import;
import io.commercestacksolutions.priceproviderservice.config.TestSecurityConfig;

@Import(TestSecurityConfig.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class QueryOperatorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @Order(1)
    public void priceRows_invalidEmbeddedComparisonOperator_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows").param("q", "priceValue:x>50"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }

    @Test
    @Order(2)
    public void priceRows_doubleGreaterOperator_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows").param("q", "priceValue:>>100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }

    @Test
    @Order(3)
    public void units_doubleColonOperator_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/admin/api/units").param("q", "measure::invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }

    @Test
    @Order(4)
    public void units_embeddedComparisonOperator_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/admin/api/units").param("q", "measure:x>50"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }
}

