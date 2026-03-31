package io.commercestacksolutions.priceproviderservice.service.publicprice.strategy;

import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.enums.PriceType;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import io.commercestacksolutions.priceproviderservice.service.group.model.GroupWithDistance;
import io.commercestacksolutions.priceproviderservice.service.publicprice.model.PriceMatchingCriteria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for DefaultPriceDeterminationStrategy to ensure correct price determination logic.
 * 
 * NOTE: As of the refactoring to use PriceCandidatesQueryStrategy,
 * the strategy now expects PRE-FILTERED candidates from the database query.
 * Tests that verify filtering logic (currency, unit, priceType, date range, quantity)
 * have been removed as this filtering is done at the DB level by the query strategy.
 * 
 * These tests focus on RANKING/SORTING which is the strategy's main responsibility.
 */
@ExtendWith(MockitoExtension.class)
public class DefaultPriceDeterminationStrategyTest {
    
    private DefaultPriceDeterminationStrategy strategy;
    
    @BeforeEach
    public void setup() {
        strategy = new DefaultPriceDeterminationStrategy();
    }
    
    @Test
    public void testDateRangeMatching_ValidDateRange() {
        // Create a price valid from yesterday to tomorrow
        PriceRowEntity price = createTestPrice("PROD-001", new BigDecimal("10.00"));
        price.setValidFrom(OffsetDateTime.now().minusDays(1));
        price.setValidTo(OffsetDateTime.now().plusDays(1));
        
        PriceMatchingCriteria criteria = createTestCriteria("PROD-001", new BigDecimal("1.00"));
        criteria.setReferenceDate(OffsetDateTime.now());
        
        PriceRowEntity result = strategy.determineBestPrice(criteria, Arrays.asList(price), List.of());
        
        assertNotNull(result);
        assertEquals(price.getId(), result.getId());
    }
    
    // REMOVED: testDateRangeMatching_BeforeValidFrom
    // This filtering is now done at DB level by PriceCandidatesQueryStrategy.
    // The strategy only receives pre-filtered candidates.
    
    // REMOVED: testDateRangeMatching_AfterValidTo
    // This filtering is now done at DB level by PriceCandidatesQueryStrategy.
    // The strategy only receives pre-filtered candidates.
    
    @Test
    public void testQuantityMatching_ExactMinQuantity() {
        PriceRowEntity price = createTestPrice("PROD-001", new BigDecimal("10.00"));
        price.setMinQuantity(new BigDecimal("10.00"));
        
        PriceMatchingCriteria criteria = createTestCriteria("PROD-001", new BigDecimal("10.00"));
        
        PriceRowEntity result = strategy.determineBestPrice(criteria, Arrays.asList(price), List.of());
        
        assertNotNull(result);
    }
    
    @Test
    public void testQuantityMatching_AboveMinQuantity() {
        PriceRowEntity price = createTestPrice("PROD-001", new BigDecimal("10.00"));
        price.setMinQuantity(new BigDecimal("10.00"));
        
        PriceMatchingCriteria criteria = createTestCriteria("PROD-001", new BigDecimal("20.00"));
        
        PriceRowEntity result = strategy.determineBestPrice(criteria, Arrays.asList(price), List.of());
        
        assertNotNull(result);
    }
    
    // REMOVED: testQuantityMatching_BelowMinQuantity
    // This filtering is now done at DB level by PriceCandidatesQueryStrategy.
    // The query only returns prices where criteria.quantity >= price.minQuantity.
    
    @Test
    public void testQuantityMatching_NearestMinQuantityWins() {
        // Create two prices with different min quantities
        PriceRowEntity price1 = createTestPrice("PROD-001", new BigDecimal("100.00"));
        price1.setId(1L);
        price1.setMinQuantity(new BigDecimal("1.00"));
        
        PriceRowEntity price2 = createTestPrice("PROD-001", new BigDecimal("90.00"));
        price2.setId(2L);
        price2.setMinQuantity(new BigDecimal("10.00"));
        
        PriceMatchingCriteria criteria = createTestCriteria("PROD-001", new BigDecimal("15.00"));
        
        PriceRowEntity result = strategy.determineBestPrice(criteria, Arrays.asList(price1, price2), List.of());
        
        assertNotNull(result);
        assertEquals(2L, result.getId(), "Higher minQuantity should win (nearest match)");
    }
    
    @Test
    public void testPriceTypeMatching_RankingWithSameType() {
        // Since DB pre-filters by priceType, all candidates will have the same type
        // This test verifies ranking works when types match
        PriceRowEntity price1 = createTestPrice("PROD-001", new BigDecimal("100.00"));
        price1.setId(1L);
        price1.setPriceType(PriceType.SALES_PRICE);
        price1.setMinQuantity(new BigDecimal("1.00"));
        
        PriceRowEntity price2 = createTestPrice("PROD-001", new BigDecimal("90.00"));
        price2.setId(2L);
        price2.setPriceType(PriceType.SALES_PRICE);
        price2.setMinQuantity(new BigDecimal("10.00"));
        
        PriceMatchingCriteria criteria = createTestCriteria("PROD-001", new BigDecimal("15.00"));
        criteria.setPriceType(PriceType.SALES_PRICE);
        
        // Both prices have same type, ranking by minQuantity (nearest higher wins)
        PriceRowEntity result = strategy.determineBestPrice(criteria, Arrays.asList(price1, price2), List.of());
        
        assertNotNull(result);
        assertEquals(2L, result.getId(), "Higher minQuantity should win");
    }
    
    // REMOVED: testCurrencyMatching_ExactMatchRequired
    // Currency filtering is now done at DB level by PriceCandidatesQueryStrategy.
    // The query only returns prices matching criteria.currencyRef.
    
    // REMOVED: testUnitMatching_ExactMatchRequired
    // Unit filtering is now done at DB level by PriceCandidatesQueryStrategy.
    // The query only returns prices matching criteria.unitRef.
    
    @Test
    public void testAllPrices_ReturnsMultipleMatches() {
        // Create three prices
        PriceRowEntity price1 = createTestPrice("PROD-001", new BigDecimal("100.00"));
        price1.setId(1L);
        price1.setMinQuantity(new BigDecimal("1.00"));
        
        PriceRowEntity price2 = createTestPrice("PROD-001", new BigDecimal("90.00"));
        price2.setId(2L);
        price2.setMinQuantity(new BigDecimal("10.00"));
        
        PriceRowEntity price3 = createTestPrice("PROD-001", new BigDecimal("80.00"));
        price3.setId(3L);
        price3.setMinQuantity(new BigDecimal("100.00"));
        
        PriceMatchingCriteria criteria = createTestCriteria("PROD-001", new BigDecimal("150.00"));
        
        List<PriceRowEntity> results = strategy.rankPrices(criteria, Arrays.asList(price1, price2, price3), List.of());
        
        assertEquals(3, results.size(), "All prices should match");
        // Verify ranking: highest minQuantity first
        assertEquals(3L, results.get(0).getId());
        assertEquals(2L, results.get(1).getId());
        assertEquals(1L, results.get(2).getId());
    }
    
    @Test
    public void testGroupDistancePriority_NearerGroupWins() {
        // Create group hierarchy: GROUP-CHILD (level 0) -> GROUP-PARENT (level 1) -> GROUP-GRANDPARENT (level 2)
        List<GroupWithDistance> groupHierarchy = Arrays.asList(
                new GroupWithDistance("GROUP-CHILD", 0),
                new GroupWithDistance("GROUP-PARENT", 1),
                new GroupWithDistance("GROUP-GRANDPARENT", 2)
        );
        
        // Price 1: Assigned to grandparent group (distance = 2)
        PriceRowEntity price1 = createTestPrice("PROD-001", new BigDecimal("100.00"));
        price1.setId(1L);
        price1.setGroupRefs(Set.of("GROUP-GRANDPARENT"));
        
        // Price 2: Assigned to parent group (distance = 1) - should win
        PriceRowEntity price2 = createTestPrice("PROD-001", new BigDecimal("95.00"));
        price2.setId(2L);
        price2.setGroupRefs(Set.of("GROUP-PARENT"));
        
        // Price 3: Assigned to child group itself (distance = 0) - should win over both
        PriceRowEntity price3 = createTestPrice("PROD-001", new BigDecimal("90.00"));
        price3.setId(3L);
        price3.setGroupRefs(Set.of("GROUP-CHILD"));
        
        PriceMatchingCriteria criteria = createTestCriteria("PROD-001", new BigDecimal("10.00"));
        criteria.setGroupId("GROUP-CHILD");
        
        List<PriceRowEntity> results = strategy.rankPrices(criteria, Arrays.asList(price1, price2, price3), groupHierarchy);
        
        // Verify ranking: nearer group wins (lower distance level)
        assertEquals(3, results.size());
        assertEquals(3L, results.get(0).getId(), "GROUP-CHILD (distance 0) should win");
        assertEquals(2L, results.get(1).getId(), "GROUP-PARENT (distance 1) should be second");
        assertEquals(1L, results.get(2).getId(), "GROUP-GRANDPARENT (distance 2) should be third");
    }
    
    @Test
    public void testGroupDistancePriority_GroupSpecificBeatsGeneric() {
        // Group hierarchy
        List<GroupWithDistance> groupHierarchy = Arrays.asList(
                new GroupWithDistance("GROUP-PREMIUM", 0),
                new GroupWithDistance("GROUP-STANDARD", 1)
        );
        
        // Price 1: Generic price (no groups) - has max distance
        PriceRowEntity price1 = createTestPrice("PROD-001", new BigDecimal("100.00"));
        price1.setId(1L);
        price1.setGroupRefs(new HashSet<>());
        
        // Price 2: Group-specific price - should win
        PriceRowEntity price2 = createTestPrice("PROD-001", new BigDecimal("85.00"));
        price2.setId(2L);
        price2.setGroupRefs(Set.of("GROUP-PREMIUM"));
        
        PriceMatchingCriteria criteria = createTestCriteria("PROD-001", new BigDecimal("10.00"));
        criteria.setGroupId("GROUP-PREMIUM");
        
        List<PriceRowEntity> results = strategy.rankPrices(criteria, Arrays.asList(price1, price2), groupHierarchy);
        
        assertEquals(2, results.size());
        assertEquals(2L, results.get(0).getId(), "Group-specific price should win over generic");
        assertEquals(1L, results.get(1).getId(), "Generic price should be second");
    }
    
    @Test
    public void testGroupDistancePriority_CombinedWithQuantity() {
        // Group hierarchy
        List<GroupWithDistance> groupHierarchy = Arrays.asList(
                new GroupWithDistance("GROUP-CHILD", 0),
                new GroupWithDistance("GROUP-PARENT", 1)
        );
        
        // Price 1: Parent group, higher minQuantity
        PriceRowEntity price1 = createTestPrice("PROD-001", new BigDecimal("90.00"));
        price1.setId(1L);
        price1.setGroupRefs(Set.of("GROUP-PARENT"));
        price1.setMinQuantity(new BigDecimal("100.00"));
        
        // Price 2: Child group, lower minQuantity - group distance wins over quantity
        PriceRowEntity price2 = createTestPrice("PROD-001", new BigDecimal("95.00"));
        price2.setId(2L);
        price2.setGroupRefs(Set.of("GROUP-CHILD"));
        price2.setMinQuantity(new BigDecimal("1.00"));
        
        PriceMatchingCriteria criteria = createTestCriteria("PROD-001", new BigDecimal("150.00"));
        criteria.setGroupId("GROUP-CHILD");
        
        List<PriceRowEntity> results = strategy.rankPrices(criteria, Arrays.asList(price1, price2), groupHierarchy);
        
        assertEquals(2, results.size());
        assertEquals(2L, results.get(0).getId(), "Group distance beats quantity in priority");
    }
    
    @Test
    public void testGroupDistancePriority_SameDistance_QuantityWins() {
        // Group hierarchy with two groups at same level (both children)
        List<GroupWithDistance> groupHierarchy = Arrays.asList(
                new GroupWithDistance("GROUP-A", 0),
                new GroupWithDistance("GROUP-B", 0)
        );
        
        // Price 1: GROUP-A, lower minQuantity
        PriceRowEntity price1 = createTestPrice("PROD-001", new BigDecimal("100.00"));
        price1.setId(1L);
        price1.setGroupRefs(Set.of("GROUP-A"));
        price1.setMinQuantity(new BigDecimal("1.00"));
        
        // Price 2: GROUP-B, higher minQuantity - should win
        PriceRowEntity price2 = createTestPrice("PROD-001", new BigDecimal("95.00"));
        price2.setId(2L);
        price2.setGroupRefs(Set.of("GROUP-B"));
        price2.setMinQuantity(new BigDecimal("50.00"));
        
        PriceMatchingCriteria criteria = createTestCriteria("PROD-001", new BigDecimal("100.00"));
        
        List<PriceRowEntity> results = strategy.rankPrices(criteria, Arrays.asList(price1, price2), groupHierarchy);
        
        assertEquals(2, results.size());
        assertEquals(2L, results.get(0).getId(), "When group distance is same, quantity wins");
    }
    
    /**
     * Helper method to create a test price row with default values.
     */
    private PriceRowEntity createTestPrice(String pricedResourceId, BigDecimal priceValue) {
        PriceRowEntity price = new PriceRowEntity();
        price.setId(1L);
        price.setPricedResourceId(pricedResourceId);
        price.setPriceValue(priceValue);
        price.setMinQuantity(new BigDecimal("1.00"));
        price.setPriceType(PriceType.SALES_PRICE);
        price.setTaxIncluded(false);
        price.setValidFrom(OffsetDateTime.now().minusDays(365));
        price.setValidTo(null); // No expiration
        price.setGroups(new HashSet<>());
        
        // Set unit
        UnitEntity unit = new UnitEntity();
        unit.setSymbol("pcs");
        price.setUnit(unit);
        
        // Set currency
        CurrencyEntity currency = new CurrencyEntity();
        currency.setCurrencyKey("EUR");
        price.setCurrency(currency);
        
        // Set tax class
        TaxClassEntity taxClass = new TaxClassEntity();
        taxClass.setTaxClassId("STANDARD");
        taxClass.setTaxRate(new BigDecimal("0.19"));
        price.setTaxClass(taxClass);
        
        return price;
    }
    
    /**
     * Helper method to create test matching criteria with default values.
     */
    private PriceMatchingCriteria createTestCriteria(String pricedResourceId, BigDecimal quantity) {
        PriceMatchingCriteria criteria = new PriceMatchingCriteria();
        criteria.setPricedResourceId(pricedResourceId);
        criteria.setQuantity(quantity);
        criteria.setUnitRef("pcs");
        criteria.setCurrencyRef("EUR");
        criteria.setPriceType(PriceType.SALES_PRICE);
        criteria.setReferenceDate(OffsetDateTime.now());
        criteria.setTaxationMode(PriceMatchingCriteria.TaxationMode.GROSS);
        return criteria;
    }
}
