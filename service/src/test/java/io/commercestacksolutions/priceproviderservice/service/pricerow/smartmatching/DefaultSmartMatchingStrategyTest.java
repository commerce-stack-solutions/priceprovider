package io.commercestacksolutions.priceproviderservice.service.pricerow.smartmatching;

import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.PriceRowEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.enums.PriceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultSmartMatchingStrategy}.
 */
@ExtendWith(MockitoExtension.class)
class DefaultSmartMatchingStrategyTest {

    @Mock
    private PriceRowEntityRepository priceRowEntityRepository;

    private DefaultSmartMatchingStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new DefaultSmartMatchingStrategy(priceRowEntityRepository);
    }

    // --- Helper to build a fully-populated context ---

    private PriceRowMatchingContext fullContext() {
        PriceRowMatchingContext ctx = new PriceRowMatchingContext();
        ctx.setPricedResourceId("PROD-1");
        ctx.setMinQuantity(BigDecimal.ONE);
        ctx.setUnitRef("kg");
        ctx.setCurrencyRef("EUR");
        ctx.setTaxClassRef("STANDARD");
        ctx.setTaxIncluded(false);
        ctx.setPriceType(PriceType.SALES_PRICE);
        return ctx;
    }

    // --- Required-field guard ---

    @Test
    void findMatch_nullPricedResourceId_returnsEmpty() {
        PriceRowMatchingContext ctx = fullContext();
        ctx.setPricedResourceId(null);
        assertTrue(strategy.findMatch(ctx).isEmpty());
    }

    @Test
    void findMatch_nullMinQuantity_returnsEmpty() {
        PriceRowMatchingContext ctx = fullContext();
        ctx.setMinQuantity(null);
        assertTrue(strategy.findMatch(ctx).isEmpty());
    }

    @Test
    void findMatch_nullUnitRef_returnsEmpty() {
        PriceRowMatchingContext ctx = fullContext();
        ctx.setUnitRef(null);
        assertTrue(strategy.findMatch(ctx).isEmpty());
    }

    @Test
    void findMatch_nullCurrencyRef_returnsEmpty() {
        PriceRowMatchingContext ctx = fullContext();
        ctx.setCurrencyRef(null);
        assertTrue(strategy.findMatch(ctx).isEmpty());
    }

    @Test
    void findMatch_nullTaxClassRef_returnsEmpty() {
        PriceRowMatchingContext ctx = fullContext();
        ctx.setTaxClassRef(null);
        assertTrue(strategy.findMatch(ctx).isEmpty());
    }

    // --- No repository candidates ---

    @Test
    @SuppressWarnings("unchecked")
    void findMatch_noCandidatesFound_returnsEmpty() {
        when(priceRowEntityRepository.findAll(any(Specification.class))).thenReturn(List.of());

        assertTrue(strategy.findMatch(fullContext()).isEmpty());
    }

    // --- Successful match without groups ---

    @Test
    @SuppressWarnings("unchecked")
    void findMatch_candidateWithNoGroups_matchesNullGroupRefs() {
        PriceRowEntity candidate = new PriceRowEntity();
        candidate.setId(42L);

        when(priceRowEntityRepository.findAll(any(Specification.class))).thenReturn(List.of(candidate));

        PriceRowMatchingContext ctx = fullContext();
        ctx.setGroupRefs(null);

        Optional<PriceRowEntity> result = strategy.findMatch(ctx);
        assertTrue(result.isPresent());
        assertEquals(42L, result.get().getId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void findMatch_candidateWithNoGroups_matchesEmptyGroupRefs() {
        PriceRowEntity candidate = new PriceRowEntity();
        candidate.setId(42L);

        when(priceRowEntityRepository.findAll(any(Specification.class))).thenReturn(List.of(candidate));

        PriceRowMatchingContext ctx = fullContext();
        ctx.setGroupRefs(Set.of());

        Optional<PriceRowEntity> result = strategy.findMatch(ctx);
        assertTrue(result.isPresent());
        assertEquals(42L, result.get().getId());
    }

    // --- Successful match with groups ---

    @Test
    @SuppressWarnings("unchecked")
    void findMatch_candidateGroupsMatchExactly_returnsCandidate() {
        GroupEntity g1 = new GroupEntity("GROUP-A");
        GroupEntity g2 = new GroupEntity("GROUP-B");

        PriceRowEntity candidate = new PriceRowEntity();
        candidate.setId(10L);
        candidate.setGroups(Set.of(g1, g2));

        when(priceRowEntityRepository.findAll(any(Specification.class))).thenReturn(List.of(candidate));

        PriceRowMatchingContext ctx = fullContext();
        ctx.setGroupRefs(Set.of("GROUP-A", "GROUP-B"));

        Optional<PriceRowEntity> result = strategy.findMatch(ctx);
        assertTrue(result.isPresent());
        assertEquals(10L, result.get().getId());
    }

    // --- Group mismatch ---

    @Test
    @SuppressWarnings("unchecked")
    void findMatch_candidateGroupsDiffer_returnsEmpty() {
        GroupEntity g1 = new GroupEntity("GROUP-A");

        PriceRowEntity candidate = new PriceRowEntity();
        candidate.setId(10L);
        candidate.setGroups(Set.of(g1));

        when(priceRowEntityRepository.findAll(any(Specification.class))).thenReturn(List.of(candidate));

        PriceRowMatchingContext ctx = fullContext();
        // Request specifies GROUP-B but candidate only has GROUP-A
        ctx.setGroupRefs(Set.of("GROUP-B"));

        assertTrue(strategy.findMatch(ctx).isEmpty());
    }

    // --- Date fields in context ---

    @Test
    @SuppressWarnings("unchecked")
    void findMatch_withValidFromAndTo_matchesCandidateWithNoGroups() {
        PriceRowEntity candidate = new PriceRowEntity();
        candidate.setId(99L);

        when(priceRowEntityRepository.findAll(any(Specification.class))).thenReturn(List.of(candidate));

        PriceRowMatchingContext ctx = fullContext();
        ctx.setValidFrom(OffsetDateTime.now().minusDays(1));
        ctx.setValidTo(OffsetDateTime.now().plusDays(1));

        Optional<PriceRowEntity> result = strategy.findMatch(ctx);
        assertTrue(result.isPresent());
        assertEquals(99L, result.get().getId());
    }
}
