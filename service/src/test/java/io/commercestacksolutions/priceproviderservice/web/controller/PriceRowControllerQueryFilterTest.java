package io.commercestacksolutions.priceproviderservice.web.controller;

import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.CurrencyEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.domain.pricetype.PriceType;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.PriceRowEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.TaxClassEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.UnitEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.GroupEntityRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.context.annotation.Import;
import io.commercestacksolutions.priceproviderservice.config.TestSecurityConfig;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

/**
 * Comprehensive query filter tests for PriceRow entity.
 * Tests all query scenarios from the Postman collection including happy and angry paths.
 * This is the critical test that reproduces the bug: currencyRef:EUR
 */
@Import(TestSecurityConfig.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PriceRowControllerQueryFilterTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PriceRowEntityRepository priceRowRepository;

    @Autowired
    private CurrencyEntityRepository currencyRepository;

    @Autowired
    private UnitEntityRepository unitRepository;

    @Autowired
    private TaxClassEntityRepository taxClassRepository;

    @Autowired
    private GroupEntityRepository groupRepository;

    private CurrencyEntity eur;
    private CurrencyEntity usd;
    private UnitEntity piece;
    private TaxClassEntity taxClass19;
    private GroupEntity groupA;

    @BeforeEach
    public void setup() {
        priceRowRepository.deleteAll();
        groupRepository.deleteAll();
        taxClassRepository.deleteAll();
        unitRepository.deleteAll();
        currencyRepository.deleteAll();

        // Create currencies
        eur = new CurrencyEntity("EUR");
        eur.setSymbol("€");
        Map<String, String> eurNames = new HashMap<>();
        eurNames.put("en", "Euro");
        eurNames.put("de", "Euro");
        eur.setName(eurNames);
        currencyRepository.save(eur);

        usd = new CurrencyEntity("USD");
        usd.setSymbol("$");
        Map<String, String> usdNames = new HashMap<>();
        usdNames.put("en", "US Dollar");
        usdNames.put("de", "US-Dollar");
        usd.setName(usdNames);
        currencyRepository.save(usd);

        // Create unit
        piece = new UnitEntity("pcs");
        Map<String, String> pieceNames = new HashMap<>();
        pieceNames.put("en", "Piece");
        pieceNames.put("de", "Stück");
        piece.setName(pieceNames);
        piece.setMeasure("count");
        unitRepository.save(piece);

        // Create tax class
        taxClass19 = new TaxClassEntity();
        taxClass19.setTaxClassId("TAX19");
        taxClass19.setTaxRate(new BigDecimal("0.19"));
        taxClassRepository.save(taxClass19);

        // Create group
        groupA = new GroupEntity();
        groupA.setPath("GROUP_A");
        groupA.setName("Group A");
        groupRepository.save(groupA);

        // Create price rows
        PriceRowEntity priceRow1 = new PriceRowEntity();
        priceRow1.setPricedResourceId("PRODUCT_001");
        priceRow1.setPriceValue(new BigDecimal("19.99"));
        priceRow1.setMinQuantity(BigDecimal.ONE);
        priceRow1.setUnit(piece);
        priceRow1.setCurrency(eur);
        priceRow1.setTaxClass(taxClass19);
        priceRow1.setPriceType(new PriceType("SALES_PRICE"));
        priceRow1.setTaxIncluded(true);
        priceRow1.setValidFrom(OffsetDateTime.now().minusDays(10));
        priceRowRepository.save(priceRow1);

        PriceRowEntity priceRow2 = new PriceRowEntity();
        priceRow2.setPricedResourceId("PRODUCT_002");
        priceRow2.setPriceValue(new BigDecimal("99.99"));
        priceRow2.setMinQuantity(BigDecimal.ONE);
        priceRow2.setUnit(piece);
        priceRow2.setCurrency(eur);
        priceRow2.setTaxClass(taxClass19);
        priceRow2.setPriceType(new PriceType("SALES_PRICE"));
        priceRow2.setTaxIncluded(true);
        Set<GroupEntity> groups2 = new HashSet<>();
        groups2.add(groupA);
        priceRow2.setGroups(groups2);
        priceRowRepository.save(priceRow2);

        PriceRowEntity priceRow3 = new PriceRowEntity();
        priceRow3.setPricedResourceId("PRODUCT_003");
        priceRow3.setPriceValue(new BigDecimal("29.99"));
        priceRow3.setMinQuantity(BigDecimal.ONE);
        priceRow3.setUnit(piece);
        priceRow3.setCurrency(usd);
        priceRow3.setTaxClass(taxClass19);
        priceRow3.setPriceType(new PriceType("SALES_PRICE"));
        priceRow3.setTaxIncluded(false);
        priceRowRepository.save(priceRow3);

        PriceRowEntity priceRow4 = new PriceRowEntity();
        priceRow4.setPricedResourceId("PRODUCT_004");
        priceRow4.setPriceValue(new BigDecimal("49.99"));
        priceRow4.setMinQuantity(new BigDecimal("5"));
        priceRow4.setUnit(piece);
        priceRow4.setCurrency(eur);
        priceRow4.setTaxClass(taxClass19);
        priceRow4.setPriceType(new PriceType("SALES_PRICE"));
        priceRow4.setTaxIncluded(false);
        priceRowRepository.save(priceRow4);

        PriceRowEntity priceRow5 = new PriceRowEntity();
        priceRow5.setPricedResourceId("PRODUCT_005");
        priceRow5.setPriceValue(new BigDecimal("149.99"));
        priceRow5.setMinQuantity(BigDecimal.ONE);
        priceRow5.setUnit(piece);
        priceRow5.setCurrency(eur);
        priceRow5.setTaxClass(taxClass19);
        priceRow5.setPriceType(new PriceType("SALES_PRICE"));
        priceRow5.setTaxIncluded(true);
        priceRow5.setValidFrom(OffsetDateTime.now().minusDays(5));
        priceRow5.setValidTo(OffsetDateTime.now().plusDays(30));
        priceRowRepository.save(priceRow5);
    }

    // ====== HAPPY PATH TESTS ======

    @Test
    @Order(1)
    public void testFilterByPriceRange() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "priceValue:[10 TO 100]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(4)))
                .andExpect(jsonPath("$.items[*].priceValue", hasItem(19.99)))
                .andExpect(jsonPath("$.items[*].priceValue", hasItem(99.99)))
                .andExpect(jsonPath("$.items[*].priceValue", hasItem(29.99)))
                .andExpect(jsonPath("$.items[*].priceValue", hasItem(49.99)));
    }

    @Test
    @Order(2)
    public void testFilterByCurrency_EUR() throws Exception {
        // THIS IS THE BUG WE FIXED: currencyRef:EUR should work now
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "currencyRef:EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(4)))
                .andExpect(jsonPath("$.items[*].currencyRef", everyItem(is("EUR"))));
    }

    @Test
    @Order(3)
    public void testFilterByCurrency_USD() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "currencyRef:USD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].currencyRef", is("USD")));
    }

    @Test
    @Order(4)
    public void testFilterByTaxIncluded() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "taxIncluded:true AND currencyRef:EUR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[*].taxIncluded", everyItem(is(true))));
    }

    @Test
    @Order(5)
    public void testComplexFilter() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "priceValue:>50 AND currencyRef:EUR AND taxIncluded:true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[*].priceValue", hasItem(99.99)))
                .andExpect(jsonPath("$.items[*].priceValue", hasItem(149.99)));
    }

    @Test
    @Order(6)
    public void testFilterWithDateRange() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "validFrom.exists:true AND priceValue:[0 TO 1000]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    @Order(7)
    public void testFilterUnitExists() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "unitRef.exists:true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)));
    }

    @Test
    @Order(8)
    public void testFilterCurrencyExists() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "currencyRef.exists:true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)));
    }

    @Test
    @Order(9)
    public void testFilterTaxClassExists() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "taxClassRef.exists:true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)));
    }

    @Test
    @Order(10)
    public void testFilterGroupsExists() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "groupRefs.exists:true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].pricedResourceId", is("PRODUCT_002")));
    }

    @Test
    @Order(11)
    public void testFilterGroupsNotExists() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "groupRefs.exists:false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(4)));
    }

    @Test
    @Order(12)
    public void testComplexReferenceQuery() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "currencyRef.exists:true AND unitRef.exists:true AND taxIncluded:true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)));
    }

    @Test
    @Order(13)
    public void testFilterByPriceValueGreaterThan() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "priceValue:>50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    @Order(14)
    public void testFilterByPriceValueLessThan() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "priceValue:<30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    @Order(15)
    public void testFilterByMinQuantity() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "minQuantity:>1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].pricedResourceId", is("PRODUCT_004")));
    }

    @Test
    @Order(16)
    public void testFilterByUnitSymbol() throws Exception {
        // Test filtering by referenced entity's ID field
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "unitRef.symbol:pcs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)));
    }

    @Test
    @Order(17)
    public void testFilterByTaxClassId() throws Exception {
        // Test filtering by referenced entity's ID field
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "taxClassRef.taxClassId:TAX19"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)));
    }

    @Test
    @Order(18)
    public void testFilterByCurrencySymbol() throws Exception {
        // Note: Special characters like € in referenced fields are not properly handled
        // This is a known limitation - the query returns 0 results
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "currencyRef.symbol:€"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(4)));
    }

    @Test
    @Order(19)
    public void testFilterValidFromExists() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "validFrom.exists:true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)));
    }

    @Test
    @Order(20)
    public void testFilterValidToExists() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "validTo.exists:true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    @Order(21)
    public void testOpenEndedRange_UpperUnbounded() throws Exception {
        // priceValue:[10 TO *] should include all priceRows with price >= 10
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "priceValue:[10 TO *]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)));
    }

    @Test
    @Order(22)
    public void testOpenEndedRange_LowerUnbounded() throws Exception {
        // priceValue:[* TO 100] should include all priceRows with price <= 100
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "priceValue:[* TO 100]"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(4)));
    }

    // ====== ANGRY PATH TESTS (Error Scenarios) ======

    @Test
    @Order(100)
    public void testInvalidFieldName() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "invalidFieldName:value"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }

    @Test
    @Order(101)
    public void testInvalidNestedField() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "unitRef.invalidNested:value"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }

    @Test
    @Order(102)
    public void testInvalidReferenceField() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "currencyRef.wrongField:value"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }

    @Test
    @Order(103)
    public void testInvalidQuerySyntax() throws Exception {
        // Empty value after colon should return Bad Request
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "priceValue:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }

    @Test
    @Order(104)
    public void testMalformedRangeQuery() throws Exception {
        // BUG: Malformed range causes a 500 server error
        // This should ideally return 400 Bad Request
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "priceValue:[10 TO]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }

    @Test
    @Order(105)
    public void testInvalidComparisonOperator() throws Exception {
        // BUG: Invalid operator causes a 500 server error
        // This should ideally return 400 Bad Request
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "priceValue:>>100"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }
}

