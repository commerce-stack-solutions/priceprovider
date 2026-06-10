package io.commercestacksolutions.priceproviderservice.web.controller;

import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.CurrencyEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.GroupEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.PriceRowEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.definitions.PriceType;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.TaxClassEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.UnitEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import io.commercestacksolutions.priceproviderservice.config.TestSecurityConfig;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for hasAny / hasAll collection membership operators on PriceRow.
 *
 * Setup:
 *   GROUP_A, GROUP_B, ORG/GROUP-SLASH (IDs with forward slash)
 *   priceRow1 → no groups
 *   priceRow2 → GROUP_A
 *   priceRow3 → GROUP_B
 *   priceRow4 → GROUP_A + GROUP_B
 *   priceRow5 → no groups
 *   priceRow6 → ORG/GROUP-SLASH (slash-containing ID)
 *   priceRow7 → ORG/GROUP-SLASH + GROUP_A (slash ID combined with plain ID)
 */
@Import(TestSecurityConfig.class)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PriceRowControllerHasAnyHasAllTest {

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

    @BeforeEach
    public void setup() {
        priceRowRepository.deleteAll();
        groupRepository.deleteAll();
        taxClassRepository.deleteAll();
        unitRepository.deleteAll();
        currencyRepository.deleteAll();

        CurrencyEntity eur = new CurrencyEntity("EUR");
        eur.setSymbol("€");
        Map<String, String> eurNames = new HashMap<>();
        eurNames.put("en", "Euro");
        currencyRepository.save(eur);

        UnitEntity piece = new UnitEntity("pcs");
        Map<String, String> pieceNames = new HashMap<>();
        pieceNames.put("en", "Piece");
        piece.setName(pieceNames);
        piece.setMeasure("count");
        unitRepository.save(piece);

        TaxClassEntity taxClass = new TaxClassEntity();
        taxClass.setTaxClassId("TAX19");
        taxClass.setTaxRate(new BigDecimal("0.19"));
        taxClassRepository.save(taxClass);

        GroupEntity groupA = new GroupEntity();
        groupA.setPath("GROUP_A");
        groupA.setName("Group A");
        groupRepository.save(groupA);

        GroupEntity groupB = new GroupEntity();
        groupB.setPath("GROUP_B");
        groupB.setName("Group B");
        groupRepository.save(groupB);

        // Group with a forward-slash in its ID (e.g., hierarchical/path-style IDs)
        GroupEntity groupSlash = new GroupEntity();
        groupSlash.setPath("ORG/GROUP-SLASH");
        groupSlash.setName("Org Group Slash");
        groupRepository.save(groupSlash);

        // priceRow1: no groups
        PriceRowEntity priceRow1 = new PriceRowEntity();
        priceRow1.setPricedResourceId("PRODUCT_001");
        priceRow1.setPriceValue(new BigDecimal("10.00"));
        priceRow1.setMinQuantity(BigDecimal.ONE);
        priceRow1.setUnit(piece);
        priceRow1.setCurrency(eur);
        priceRow1.setTaxClass(taxClass);
        priceRow1.setPriceType(new PriceType("SALES_PRICE"));
        priceRow1.setTaxIncluded(true);
        priceRowRepository.save(priceRow1);

        // priceRow2: GROUP_A
        PriceRowEntity priceRow2 = new PriceRowEntity();
        priceRow2.setPricedResourceId("PRODUCT_002");
        priceRow2.setPriceValue(new BigDecimal("20.00"));
        priceRow2.setMinQuantity(BigDecimal.ONE);
        priceRow2.setUnit(piece);
        priceRow2.setCurrency(eur);
        priceRow2.setTaxClass(taxClass);
        priceRow2.setPriceType(new PriceType("SALES_PRICE"));
        priceRow2.setTaxIncluded(true);
        Set<GroupEntity> groupsA = new HashSet<>();
        groupsA.add(groupA);
        priceRow2.setGroups(groupsA);
        priceRowRepository.save(priceRow2);

        // priceRow3: GROUP_B
        PriceRowEntity priceRow3 = new PriceRowEntity();
        priceRow3.setPricedResourceId("PRODUCT_003");
        priceRow3.setPriceValue(new BigDecimal("30.00"));
        priceRow3.setMinQuantity(BigDecimal.ONE);
        priceRow3.setUnit(piece);
        priceRow3.setCurrency(eur);
        priceRow3.setTaxClass(taxClass);
        priceRow3.setPriceType(new PriceType("SALES_PRICE"));
        priceRow3.setTaxIncluded(false);
        Set<GroupEntity> groupsB = new HashSet<>();
        groupsB.add(groupB);
        priceRow3.setGroups(groupsB);
        priceRowRepository.save(priceRow3);

        // priceRow4: GROUP_A + GROUP_B
        PriceRowEntity priceRow4 = new PriceRowEntity();
        priceRow4.setPricedResourceId("PRODUCT_004");
        priceRow4.setPriceValue(new BigDecimal("40.00"));
        priceRow4.setMinQuantity(BigDecimal.ONE);
        priceRow4.setUnit(piece);
        priceRow4.setCurrency(eur);
        priceRow4.setTaxClass(taxClass);
        priceRow4.setPriceType(new PriceType("SALES_PRICE"));
        priceRow4.setTaxIncluded(true);
        Set<GroupEntity> groupsAB = new HashSet<>();
        groupsAB.add(groupA);
        groupsAB.add(groupB);
        priceRow4.setGroups(groupsAB);
        priceRowRepository.save(priceRow4);

        // priceRow5: no groups
        PriceRowEntity priceRow5 = new PriceRowEntity();
        priceRow5.setPricedResourceId("PRODUCT_005");
        priceRow5.setPriceValue(new BigDecimal("50.00"));
        priceRow5.setMinQuantity(BigDecimal.ONE);
        priceRow5.setUnit(piece);
        priceRow5.setCurrency(eur);
        priceRow5.setTaxClass(taxClass);
        priceRow5.setPriceType(new PriceType("SALES_PRICE"));
        priceRow5.setTaxIncluded(false);
        priceRowRepository.save(priceRow5);

        // priceRow6: ORG/GROUP-SLASH only (slash-containing ID)
        PriceRowEntity priceRow6 = new PriceRowEntity();
        priceRow6.setPricedResourceId("PRODUCT_006");
        priceRow6.setPriceValue(new BigDecimal("60.00"));
        priceRow6.setMinQuantity(BigDecimal.ONE);
        priceRow6.setUnit(piece);
        priceRow6.setCurrency(eur);
        priceRow6.setTaxClass(taxClass);
        priceRow6.setPriceType(new PriceType("SALES_PRICE"));
        priceRow6.setTaxIncluded(true);
        Set<GroupEntity> groupsSlash = new HashSet<>();
        groupsSlash.add(groupSlash);
        priceRow6.setGroups(groupsSlash);
        priceRowRepository.save(priceRow6);

        // priceRow7: ORG/GROUP-SLASH + GROUP_A (slash ID combined with plain ID)
        PriceRowEntity priceRow7 = new PriceRowEntity();
        priceRow7.setPricedResourceId("PRODUCT_007");
        priceRow7.setPriceValue(new BigDecimal("70.00"));
        priceRow7.setMinQuantity(BigDecimal.ONE);
        priceRow7.setUnit(piece);
        priceRow7.setCurrency(eur);
        priceRow7.setTaxClass(taxClass);
        priceRow7.setPriceType(new PriceType("SALES_PRICE"));
        priceRow7.setTaxIncluded(true);
        Set<GroupEntity> groupsSlashA = new HashSet<>();
        groupsSlashA.add(groupSlash);
        groupsSlashA.add(groupA);
        priceRow7.setGroups(groupsSlashA);
        priceRowRepository.save(priceRow7);
    }

    // ====== hasAny HAPPY PATH ======

    @Test
    @Order(1)
    public void testHasAnySingleGroup_returnsMatchingRows() throws Exception {
        // groupRefs.hasAny:(GROUP_A) → priceRow2, priceRow4, priceRow7
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "groupRefs.hasAny:(GROUP_A)"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[*].pricedResourceId",
                        containsInAnyOrder("PRODUCT_002", "PRODUCT_004", "PRODUCT_007")));
    }

    @Test
    @Order(2)
    public void testHasAnyTwoGroups_returnsAllWithEitherGroup() throws Exception {
        // groupRefs.hasAny:(GROUP_A,GROUP_B) → priceRow2, priceRow3, priceRow4, priceRow7
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "groupRefs.hasAny:(GROUP_A,GROUP_B)"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(4)))
                .andExpect(jsonPath("$.items[*].pricedResourceId",
                        containsInAnyOrder("PRODUCT_002", "PRODUCT_003", "PRODUCT_004", "PRODUCT_007")));
    }

    @Test
    @Order(3)
    public void testHasAnyNonExistingGroup_returnsEmpty() throws Exception {
        // groupRefs.hasAny:(UNKNOWN) → no matches
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "groupRefs.hasAny:(UNKNOWN)"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    // ====== hasAll HAPPY PATH ======

    @Test
    @Order(10)
    public void testHasAllSingleGroup_returnsRowsContainingThatGroup() throws Exception {
        // groupRefs.hasAll:(GROUP_A) → priceRow2, priceRow4, priceRow7
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "groupRefs.hasAll:(GROUP_A)"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[*].pricedResourceId",
                        containsInAnyOrder("PRODUCT_002", "PRODUCT_004", "PRODUCT_007")));
    }

    @Test
    @Order(11)
    public void testHasAllBothGroups_returnsOnlyRowWithBothGroups() throws Exception {
        // groupRefs.hasAll:(GROUP_A,GROUP_B) → only priceRow4
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "groupRefs.hasAll:(GROUP_A,GROUP_B)"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].pricedResourceId", is("PRODUCT_004")));
    }

    @Test
    @Order(12)
    public void testHasAllNonExistingGroup_returnsEmpty() throws Exception {
        // groupRefs.hasAll:(UNKNOWN) → no matches
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "groupRefs.hasAll:(UNKNOWN)"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    // ====== Combined with other filters ======

    @Test
    @Order(20)
    public void testHasAnyCombinedWithScalarFilter() throws Exception {
        // groupRefs.hasAny:(GROUP_A,GROUP_B) AND taxIncluded:true → priceRow2, priceRow4, priceRow7
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "groupRefs.hasAny:(GROUP_A,GROUP_B) AND taxIncluded:true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(3)))
                .andExpect(jsonPath("$.items[*].pricedResourceId",
                        containsInAnyOrder("PRODUCT_002", "PRODUCT_004", "PRODUCT_007")));
    }

    @Test
    @Order(21)
    public void testNotHasAll_returnsAllExceptBothGroupRow() throws Exception {
        // NOT groupRefs.hasAll:(GROUP_A,GROUP_B) → all except priceRow4 (7 total - 1 = 6)
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "NOT groupRefs.hasAll:(GROUP_A,GROUP_B)"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(6)))
                .andExpect(jsonPath("$.items[*].pricedResourceId",
                        not(hasItem("PRODUCT_004"))));
    }

    // ====== ANGRY PATH (error scenarios) ======

    @Test
    @Order(100)
    public void testHasAnyEmptyList_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "groupRefs.hasAny:()"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(101)
    public void testHasAnyMissingParens_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "groupRefs.hasAny:GROUP_A,GROUP_B"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(102)
    public void testHasAnyOnNonCollectionField_returnsBadRequest() throws Exception {
        // priceValue is not a collection – hasAny should be rejected
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "priceValue.hasAny:(10,20)"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }

    @Test
    @Order(103)
    public void testHasAllOnNonCollectionField_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "taxIncluded.hasAll:(true,false)"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.$messages[0].message-key", notNullValue()));
    }

    // ====== Forward slash in group ID (regression tests for sanitizer stripping /) ======

    @Test
    @Order(200)
    public void testHasAnyWithSlashId_returnsMatchingRows() throws Exception {
        // groupRefs.hasAny:(ORG/GROUP-SLASH) → priceRow6, priceRow7
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "groupRefs.hasAny:(ORG/GROUP-SLASH)"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[*].pricedResourceId",
                        containsInAnyOrder("PRODUCT_006", "PRODUCT_007")));
    }

    @Test
    @Order(201)
    public void testHasAnyWithSlashIdAndPlainId_returnsMatchingRows() throws Exception {
        // groupRefs.hasAny:(ORG/GROUP-SLASH,GROUP_B) → priceRow3, priceRow4, priceRow6, priceRow7
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "groupRefs.hasAny:(ORG/GROUP-SLASH,GROUP_B)"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(4)))
                .andExpect(jsonPath("$.items[*].pricedResourceId",
                        containsInAnyOrder("PRODUCT_003", "PRODUCT_004", "PRODUCT_006", "PRODUCT_007")));
    }

    @Test
    @Order(202)
    public void testHasAllWithSlashIdAndPlainId_returnsOnlyRowWithBoth() throws Exception {
        // groupRefs.hasAll:(ORG/GROUP-SLASH,GROUP_A) → only priceRow7
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "groupRefs.hasAll:(ORG/GROUP-SLASH,GROUP_A)"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].pricedResourceId", is("PRODUCT_007")));
    }

    @Test
    @Order(203)
    public void testHasAnyWithUnknownSlashId_returnsEmpty() throws Exception {
        // A slash-ID that does not exist should return zero results, not an error
        mockMvc.perform(get("/admin/api/pricerows")
                        .param("q", "groupRefs.hasAny:(ORG/NONEXISTENT)"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }
}
