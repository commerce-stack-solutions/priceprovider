package io.commercestacksolutions.priceproviderservice.web.controller;

import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.CurrencyEntityRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;
import org.springframework.context.annotation.Import;
import io.commercestacksolutions.priceproviderservice.config.TestSecurityConfig;

/**
 * Query filter tests for Currency entity.
 */
@Import(TestSecurityConfig.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class CurrencyControllerQueryFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CurrencyEntityRepository currencyRepository;

    @BeforeEach
    public void setup() {
        currencyRepository.deleteAll();

        // Create test currencies
        CurrencyEntity eur = new CurrencyEntity("EUR");
        eur.setSymbol("€");
        Map<String, String> eurNames = new HashMap<>();
        eurNames.put("en", "Euro");
        eurNames.put("de", "Euro");
        eur.setName(eurNames);
        currencyRepository.save(eur);

        CurrencyEntity usd = new CurrencyEntity("USD");
        usd.setSymbol("$");
        Map<String, String> usdNames = new HashMap<>();
        usdNames.put("en", "US Dollar");
        usdNames.put("de", "US-Dollar");
        usd.setName(usdNames);
        currencyRepository.save(usd);

        CurrencyEntity gbp = new CurrencyEntity("GBP");
        gbp.setSymbol("£");
        Map<String, String> gbpNames = new HashMap<>();
        gbpNames.put("en", "British Pound");
        gbpNames.put("de", "Britisches Pfund");
        gbp.setName(gbpNames);
        currencyRepository.save(gbp);

        CurrencyEntity jpy = new CurrencyEntity("JPY");
        jpy.setSymbol("¥");
        Map<String, String> jpyNames = new HashMap<>();
        jpyNames.put("en", "Japanese Yen");
        jpyNames.put("de", "Japanischer Yen");
        jpy.setName(jpyNames);
        currencyRepository.save(jpy);
    }

    // ====== HAPPY PATH TESTS ======

    @Test
    @Order(1)
    public void testGetAllCurrencies() throws Exception {
        mockMvc.perform(get("/admin/api/currencies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(4)));
    }

    @Test
    @Order(2)
    public void testFilterByCurrencyKey() throws Exception {
        mockMvc.perform(get("/admin/api/currencies")
                        .param("q", "currencyKey:EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].currencyKey", is("EUR")));
    }

    @Test
    @Order(3)
    public void testFilterBySymbol() throws Exception {
        // Special characters like $ are now supported and should return the USD entry
        mockMvc.perform(get("/admin/api/currencies")
                        .param("q", "symbol:$"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].currencyKey", hasItem("USD")));
    }

    @Test
    @Order(4)
    public void testFilterBySymbolEuro() throws Exception {
        // Special characters like € are now supported and should return the EUR entry
        mockMvc.perform(get("/admin/api/currencies")
                        .param("q", "symbol:€"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[*].currencyKey", hasItem("EUR")));
    }

    @Test
    @Order(5)
    public void testSorting() throws Exception {
        mockMvc.perform(get("/admin/api/currencies")
                        .param("sort-by", "currencyKey")
                        .param("sort-direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(4)))
                .andExpect(jsonPath("$.items[0].currencyKey", is("EUR")))
                .andExpect(jsonPath("$.items[1].currencyKey", is("GBP")))
                .andExpect(jsonPath("$.items[2].currencyKey", is("JPY")))
                .andExpect(jsonPath("$.items[3].currencyKey", is("USD")));
    }

    // ====== ANGRY PATH TESTS ======

    @Test
    @Order(100)
    public void testInvalidFieldName() throws Exception {
        mockMvc.perform(get("/admin/api/currencies")
                        .param("q", "invalidField:value"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }

    @Test
    @Order(101)
    public void testInvalidNestedField() throws Exception {
        mockMvc.perform(get("/admin/api/currencies")
                        .param("q", "name.invalidNested:value"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }

    @Test
    @Order(102)
    public void testGetNonExistingCurrency() throws Exception {
        mockMvc.perform(get("/admin/api/currencies/XXX"))
                .andExpect(status().isNotFound());
    }
}

