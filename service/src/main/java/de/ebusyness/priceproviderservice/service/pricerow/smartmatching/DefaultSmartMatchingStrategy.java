package de.ebusyness.priceproviderservice.service.pricerow.smartmatching;

import de.ebusyness.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.PriceRowEntityRepository;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.enums.PriceType;
import de.ebusyness.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import de.ebusyness.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import de.ebusyness.priceproviderservice.service.currency.CurrencyService;
import de.ebusyness.priceproviderservice.service.taxclass.TaxClassService;
import de.ebusyness.priceproviderservice.service.unit.UnitService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link SmartMatchingStrategy}.
 *
 * <p>Matches price rows by <em>all</em> of the following identifying fields:
 * {@code pricedResourceId}, {@code minQuantity}, {@code unitRef}, {@code currencyRef},
 * {@code taxClassRef}, {@code taxIncluded}, {@code priceType}, {@code validFrom},
 * {@code validTo} and {@code groupRefs}.
 *
 * <p>To provide custom matching logic, declare a Spring bean that implements
 * {@link SmartMatchingStrategy} and mark it with {@code @Primary} to ensure it takes
 * precedence over this default.
 */
@Component
public class DefaultSmartMatchingStrategy implements SmartMatchingStrategy {

    private final PriceRowEntityRepository priceRowEntityRepository;
    private final UnitService unitService;
    private final CurrencyService currencyService;
    private final TaxClassService taxClassService;

    public DefaultSmartMatchingStrategy(
            PriceRowEntityRepository priceRowEntityRepository,
            UnitService unitService,
            CurrencyService currencyService,
            TaxClassService taxClassService) {
        this.priceRowEntityRepository = priceRowEntityRepository;
        this.unitService = unitService;
        this.currencyService = currencyService;
        this.taxClassService = taxClassService;
    }

    @Override
    public Optional<PriceRowEntity> findMatch(
            String pricedResourceId,
            BigDecimal minQuantity,
            String unitRef,
            String currencyRef,
            String taxClassRef,
            boolean taxIncluded,
            PriceType priceType,
            OffsetDateTime validFrom,
            OffsetDateTime validTo,
            Set<String> groupRefs) {

        // If any required field is null, return empty
        if (pricedResourceId == null || minQuantity == null || unitRef == null
                || currencyRef == null || taxClassRef == null) {
            return Optional.empty();
        }

        // Fetch the related entities
        UnitEntity unit = unitService.findBySymbol(unitRef);
        CurrencyEntity currency = currencyService.getCurrency(currencyRef);
        TaxClassEntity taxClass = taxClassService.getTaxClass(taxClassRef);

        // If any related entity doesn't exist, return empty
        if (unit == null || currency == null || taxClass == null) {
            return Optional.empty();
        }

        // Find candidates by scalar fields (groups are filtered separately)
        List<PriceRowEntity> candidates = priceRowEntityRepository.findByMatchingFields(
                pricedResourceId,
                minQuantity,
                unit,
                currency,
                taxClass,
                taxIncluded,
                priceType,
                validFrom,
                validTo
        );

        // Filter by group refs – the sets must match exactly
        Set<String> normalizedGroupRefs = groupRefs != null ? groupRefs : new HashSet<>();

        for (PriceRowEntity candidate : candidates) {
            Set<String> candidateGroupRefs = candidate.getGroups() != null
                    ? candidate.getGroups().stream()
                            .map(group -> group.getId())
                            .collect(Collectors.toSet())
                    : new HashSet<>();

            if (normalizedGroupRefs.equals(candidateGroupRefs)) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }
}
