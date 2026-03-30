package de.ebusyness.priceproviderservice.service.pricerow.smartmatching;

import de.ebusyness.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import de.ebusyness.priceproviderservice.dataaccess.group.entity.GroupEntity;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.PriceRowEntityRepository;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.enums.PriceType;
import de.ebusyness.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import de.ebusyness.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import de.ebusyness.priceproviderservice.service.currency.CurrencyService;
import de.ebusyness.priceproviderservice.service.taxclass.TaxClassService;
import de.ebusyness.priceproviderservice.service.unit.UnitService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultSmartMatchingStrategy}.
 */
@ExtendWith(MockitoExtension.class)
class DefaultSmartMatchingStrategyTest {

    @Mock
    private PriceRowEntityRepository priceRowEntityRepository;
    @Mock
    private UnitService unitService;
    @Mock
    private CurrencyService currencyService;
    @Mock
    private TaxClassService taxClassService;

    private DefaultSmartMatchingStrategy strategy;

    private UnitEntity testUnit;
    private CurrencyEntity testCurrency;
    private TaxClassEntity testTaxClass;

    @BeforeEach
    void setUp() {
        strategy = new DefaultSmartMatchingStrategy(
                priceRowEntityRepository, unitService, currencyService, taxClassService);

        testUnit = new UnitEntity();
        testUnit.setSymbol("kg");

        testCurrency = new CurrencyEntity();
        testCurrency.setCurrencyKey("EUR");

        testTaxClass = new TaxClassEntity();
        testTaxClass.setTaxClassId("STANDARD");
    }

    // --- Required-field guard ---

    @Test
    void findMatch_nullPricedResourceId_returnsEmpty() {
        Optional<PriceRowEntity> result = strategy.findMatch(
                null, BigDecimal.ONE, "kg", "EUR", "STANDARD", false, PriceType.SALES_PRICE,
                null, null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void findMatch_nullMinQuantity_returnsEmpty() {
        Optional<PriceRowEntity> result = strategy.findMatch(
                "PROD-1", null, "kg", "EUR", "STANDARD", false, PriceType.SALES_PRICE,
                null, null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void findMatch_nullUnitRef_returnsEmpty() {
        Optional<PriceRowEntity> result = strategy.findMatch(
                "PROD-1", BigDecimal.ONE, null, "EUR", "STANDARD", false, PriceType.SALES_PRICE,
                null, null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void findMatch_nullCurrencyRef_returnsEmpty() {
        Optional<PriceRowEntity> result = strategy.findMatch(
                "PROD-1", BigDecimal.ONE, "kg", null, "STANDARD", false, PriceType.SALES_PRICE,
                null, null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void findMatch_nullTaxClassRef_returnsEmpty() {
        Optional<PriceRowEntity> result = strategy.findMatch(
                "PROD-1", BigDecimal.ONE, "kg", "EUR", null, false, PriceType.SALES_PRICE,
                null, null, null);
        assertTrue(result.isEmpty());
    }

    // --- Referenced-entity not found ---

    @Test
    void findMatch_unitNotFound_returnsEmpty() {
        when(unitService.findBySymbol("kg")).thenReturn(null);
        when(currencyService.getCurrency("EUR")).thenReturn(testCurrency);
        when(taxClassService.getTaxClass("STANDARD")).thenReturn(testTaxClass);

        Optional<PriceRowEntity> result = strategy.findMatch(
                "PROD-1", BigDecimal.ONE, "kg", "EUR", "STANDARD", false, PriceType.SALES_PRICE,
                null, null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void findMatch_currencyNotFound_returnsEmpty() {
        when(unitService.findBySymbol("kg")).thenReturn(testUnit);
        when(currencyService.getCurrency("EUR")).thenReturn(null);
        when(taxClassService.getTaxClass("STANDARD")).thenReturn(testTaxClass);

        Optional<PriceRowEntity> result = strategy.findMatch(
                "PROD-1", BigDecimal.ONE, "kg", "EUR", "STANDARD", false, PriceType.SALES_PRICE,
                null, null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void findMatch_taxClassNotFound_returnsEmpty() {
        when(unitService.findBySymbol("kg")).thenReturn(testUnit);
        when(currencyService.getCurrency("EUR")).thenReturn(testCurrency);
        when(taxClassService.getTaxClass("STANDARD")).thenReturn(null);

        Optional<PriceRowEntity> result = strategy.findMatch(
                "PROD-1", BigDecimal.ONE, "kg", "EUR", "STANDARD", false, PriceType.SALES_PRICE,
                null, null, null);
        assertTrue(result.isEmpty());
    }

    // --- No repository candidates ---

    @Test
    void findMatch_noCandidatesFound_returnsEmpty() {
        when(unitService.findBySymbol("kg")).thenReturn(testUnit);
        when(currencyService.getCurrency("EUR")).thenReturn(testCurrency);
        when(taxClassService.getTaxClass("STANDARD")).thenReturn(testTaxClass);
        when(priceRowEntityRepository.findByMatchingFields(
                any(), any(), any(), any(), any(), eq(false), any(), any(), any()))
                .thenReturn(List.of());

        Optional<PriceRowEntity> result = strategy.findMatch(
                "PROD-1", BigDecimal.ONE, "kg", "EUR", "STANDARD", false, PriceType.SALES_PRICE,
                null, null, null);
        assertTrue(result.isEmpty());
    }

    // --- Successful match without groups ---

    @Test
    void findMatch_candidateWithNoGroups_matchesNullGroupRefs() {
        PriceRowEntity candidate = new PriceRowEntity();
        candidate.setId(42L);

        when(unitService.findBySymbol("kg")).thenReturn(testUnit);
        when(currencyService.getCurrency("EUR")).thenReturn(testCurrency);
        when(taxClassService.getTaxClass("STANDARD")).thenReturn(testTaxClass);
        when(priceRowEntityRepository.findByMatchingFields(
                any(), any(), any(), any(), any(), eq(false), any(), any(), any()))
                .thenReturn(List.of(candidate));

        Optional<PriceRowEntity> result = strategy.findMatch(
                "PROD-1", BigDecimal.ONE, "kg", "EUR", "STANDARD", false, PriceType.SALES_PRICE,
                null, null, null);

        assertTrue(result.isPresent());
        assertEquals(42L, result.get().getId());
    }

    @Test
    void findMatch_candidateWithNoGroups_matchesEmptyGroupRefs() {
        PriceRowEntity candidate = new PriceRowEntity();
        candidate.setId(42L);

        when(unitService.findBySymbol("kg")).thenReturn(testUnit);
        when(currencyService.getCurrency("EUR")).thenReturn(testCurrency);
        when(taxClassService.getTaxClass("STANDARD")).thenReturn(testTaxClass);
        when(priceRowEntityRepository.findByMatchingFields(
                any(), any(), any(), any(), any(), eq(false), any(), any(), any()))
                .thenReturn(List.of(candidate));

        Optional<PriceRowEntity> result = strategy.findMatch(
                "PROD-1", BigDecimal.ONE, "kg", "EUR", "STANDARD", false, PriceType.SALES_PRICE,
                null, null, Set.of());

        assertTrue(result.isPresent());
        assertEquals(42L, result.get().getId());
    }

    // --- Successful match with groups ---

    @Test
    void findMatch_candidateGroupsMatchExactly_returnsCandidate() {
        GroupEntity g1 = new GroupEntity("GROUP-A");
        GroupEntity g2 = new GroupEntity("GROUP-B");

        PriceRowEntity candidate = new PriceRowEntity();
        candidate.setId(10L);
        candidate.setGroups(Set.of(g1, g2));

        when(unitService.findBySymbol("kg")).thenReturn(testUnit);
        when(currencyService.getCurrency("EUR")).thenReturn(testCurrency);
        when(taxClassService.getTaxClass("STANDARD")).thenReturn(testTaxClass);
        when(priceRowEntityRepository.findByMatchingFields(
                any(), any(), any(), any(), any(), eq(false), any(), any(), any()))
                .thenReturn(List.of(candidate));

        Optional<PriceRowEntity> result = strategy.findMatch(
                "PROD-1", BigDecimal.ONE, "kg", "EUR", "STANDARD", false, PriceType.SALES_PRICE,
                null, null, Set.of("GROUP-A", "GROUP-B"));

        assertTrue(result.isPresent());
        assertEquals(10L, result.get().getId());
    }

    // --- Group mismatch ---

    @Test
    void findMatch_candidateGroupsDiffer_returnsEmpty() {
        GroupEntity g1 = new GroupEntity("GROUP-A");

        PriceRowEntity candidate = new PriceRowEntity();
        candidate.setId(10L);
        candidate.setGroups(Set.of(g1));

        when(unitService.findBySymbol("kg")).thenReturn(testUnit);
        when(currencyService.getCurrency("EUR")).thenReturn(testCurrency);
        when(taxClassService.getTaxClass("STANDARD")).thenReturn(testTaxClass);
        when(priceRowEntityRepository.findByMatchingFields(
                any(), any(), any(), any(), any(), eq(false), any(), any(), any()))
                .thenReturn(List.of(candidate));

        // Request specifies GROUP-B but candidate only has GROUP-A
        Optional<PriceRowEntity> result = strategy.findMatch(
                "PROD-1", BigDecimal.ONE, "kg", "EUR", "STANDARD", false, PriceType.SALES_PRICE,
                null, null, Set.of("GROUP-B"));

        assertTrue(result.isEmpty());
    }

    // --- Date fields are forwarded to the repository ---

    @Test
    void findMatch_withValidFromAndTo_matchesCandidateWithNoGroups() {
        OffsetDateTime validFrom = OffsetDateTime.now().minusDays(1);
        OffsetDateTime validTo = OffsetDateTime.now().plusDays(1);

        PriceRowEntity candidate = new PriceRowEntity();
        candidate.setId(99L);

        when(unitService.findBySymbol("kg")).thenReturn(testUnit);
        when(currencyService.getCurrency("EUR")).thenReturn(testCurrency);
        when(taxClassService.getTaxClass("STANDARD")).thenReturn(testTaxClass);
        when(priceRowEntityRepository.findByMatchingFields(
                eq("PROD-1"), any(), eq(testUnit), eq(testCurrency), eq(testTaxClass),
                eq(false), eq(PriceType.SALES_PRICE), eq(validFrom), eq(validTo)))
                .thenReturn(List.of(candidate));

        Optional<PriceRowEntity> result = strategy.findMatch(
                "PROD-1", BigDecimal.ONE, "kg", "EUR", "STANDARD", false, PriceType.SALES_PRICE,
                validFrom, validTo, null);

        assertTrue(result.isPresent());
        assertEquals(99L, result.get().getId());
    }
}
