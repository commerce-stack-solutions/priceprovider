package de.ebusyness.priceproviderservice.web.controller;

import de.ebusyness.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import de.ebusyness.priceproviderservice.dataaccess.taxclass.TaxClassEntityRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import de.ebusyness.priceproviderservice.config.TestSecurityConfig;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Comprehensive query filter tests for TaxClass entity.
 */
@Import(TestSecurityConfig.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class TaxClassControllerQueryFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaxClassEntityRepository taxClassRepository;

    @BeforeEach
    public void setup() {
        taxClassRepository.deleteAll();

        // Create test tax classes
        TaxClassEntity tax19 = new TaxClassEntity();
        tax19.setTaxClassId("TAX19");
        tax19.setTaxRate(new BigDecimal("19.00"));
        taxClassRepository.save(tax19);

        TaxClassEntity tax7 = new TaxClassEntity();
        tax7.setTaxClassId("TAX7");
        tax7.setTaxRate(new BigDecimal("7.00"));
        taxClassRepository.save(tax7);

        TaxClassEntity tax0 = new TaxClassEntity();
        tax0.setTaxClassId("TAX0");
        tax0.setTaxRate(BigDecimal.ZERO);
        taxClassRepository.save(tax0);

        TaxClassEntity tax20 = new TaxClassEntity();
        tax20.setTaxClassId("TAX20");
        tax20.setTaxRate(new BigDecimal("20.00"));
        taxClassRepository.save(tax20);
    }

    // ====== HAPPY PATH TESTS ======

    @Test
    @Order(1)
    public void testGetAllTaxClasses() throws Exception {
        mockMvc.perform(get("/admin/api/taxclasses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(4)));
    }

    @Test
    @Order(2)
    public void testFilterByTaxClassId() throws Exception {
        mockMvc.perform(get("/admin/api/taxclasses")
                        .param("q", "taxClassId:TAX19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].taxClassId", is("TAX19")));
    }

    @Test
    @Order(3)
    public void testFilterByRateRange() throws Exception {
        mockMvc.perform(get("/admin/api/taxclasses")
                        .param("q", "taxRate:[5.00 TO 15.00]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].taxClassId", is("TAX7")));
    }

    @Test
    @Order(4)
    public void testFilterByRateGreaterThan() throws Exception {
        mockMvc.perform(get("/admin/api/taxclasses")
                        .param("q", "taxRate:>15.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[*].taxClassId", hasItems("TAX19", "TAX20")));
    }

    @Test
    @Order(5)
    public void testFilterByRateLessThan() throws Exception {
        mockMvc.perform(get("/admin/api/taxclasses")
                        .param("q", "taxRate:<10.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[*].taxClassId", hasItems("TAX0", "TAX7")));
    }

    @Test
    @Order(6)
    public void testFilterByRateEquals() throws Exception {
        mockMvc.perform(get("/admin/api/taxclasses")
                        .param("q", "taxRate:19.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].taxClassId", is("TAX19")));
    }

    @Test
    @Order(7)
    public void testFilterByRateZero() throws Exception {
        mockMvc.perform(get("/admin/api/taxclasses")
                        .param("q", "taxRate:0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].taxClassId", is("TAX0")));
    }

    @Test
    @Order(8)
    public void testSortByRate() throws Exception {
        mockMvc.perform(get("/admin/api/taxclasses")
                        .param("sort-by", "taxRate")
                        .param("sort-direction", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(4)))
                .andExpect(jsonPath("$.items[0].taxClassId", is("TAX0")))
                .andExpect(jsonPath("$.items[1].taxClassId", is("TAX7")))
                .andExpect(jsonPath("$.items[2].taxClassId", is("TAX19")))
                .andExpect(jsonPath("$.items[3].taxClassId", is("TAX20")));
    }

    @Test
    @Order(9)
    public void testSortByRateDescending() throws Exception {
        mockMvc.perform(get("/admin/api/taxclasses")
                        .param("sort-by", "taxRate")
                        .param("sort-direction", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(4)))
                .andExpect(jsonPath("$.items[0].taxClassId", is("TAX20")))
                .andExpect(jsonPath("$.items[1].taxClassId", is("TAX19")))
                .andExpect(jsonPath("$.items[2].taxClassId", is("TAX7")))
                .andExpect(jsonPath("$.items[3].taxClassId", is("TAX0")));
    }

    // ====== ANGRY PATH TESTS ======

    @Test
    @Order(100)
    public void testInvalidFieldName() throws Exception {
        mockMvc.perform(get("/admin/api/taxclasses")
                        .param("q", "invalidField:value"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }

    @Test
    @Order(101)
    public void testInvalidNestedField() throws Exception {
        mockMvc.perform(get("/admin/api/taxclasses")
                        .param("q", "name.invalidNested:value"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }

    @Test
    @Order(102)
    public void testGetNonExistingTaxClass() throws Exception {
        mockMvc.perform(get("/admin/api/taxclasses/TAXXX"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(104)
    public void testInvalidRateFormat() throws Exception {
        // BUG: Invalid format causes a 500 server error
        // Parser now validates types and should return 400 Bad Request
        mockMvc.perform(get("/admin/api/taxclasses")
                        .param("q", "taxRate:not-a-number"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(105)
    public void testMalformedRangeQuery() throws Exception {
        // BUG: Malformed range causes a 500 server error
        // This should ideally return 400 Bad Request
        mockMvc.perform(get("/admin/api/taxclasses")
                        .param("q", "taxRate:[10.00 TO]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }

    @Test
    @Order(106)
    public void testOpenEndedRange_TaxRate_UpperUnbounded() throws Exception {
        // taxRate:[10.00 TO *] should include tax rates >= 10.00 (TAX19=19.00 and TAX20=20.00)
        mockMvc.perform(get("/admin/api/taxclasses")
                        .param("q", "taxRate:[10.00 TO *]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    @Order(107)
    public void testOpenEndedRange_TaxRate_LowerUnbounded() throws Exception {
        // taxRate:[* TO 15.00] should include tax rates <= 15.00 (TAX7=7.00 and TAX0=0.00)
        mockMvc.perform(get("/admin/api/taxclasses")
                        .param("q", "taxRate:[* TO 15.00]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)));
    }
}
