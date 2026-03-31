package de.ebusyness.priceproviderservice.service.pricerow.smartmatching;

import de.ebusyness.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import de.ebusyness.priceproviderservice.dataaccess.group.entity.GroupEntity;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.PriceRowEntityRepository;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import de.ebusyness.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import de.ebusyness.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
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
 * <p>The database query is built dynamically as a JPA {@link Specification} so that each
 * field can be handled independently (including nullable date and enum fields).  Entity
 * references (unit, currency, tax class) are resolved via JOIN on their natural keys,
 * avoiding the need to pre-fetch those entities from separate services.
 *
 * <p>To provide custom matching logic, declare a Spring bean that implements
 * {@link SmartMatchingStrategy} and mark it with {@code @Primary} to ensure it takes
 * precedence over this default.
 */
@Component
public class DefaultSmartMatchingStrategy implements SmartMatchingStrategy {

    private final PriceRowEntityRepository priceRowEntityRepository;

    public DefaultSmartMatchingStrategy(PriceRowEntityRepository priceRowEntityRepository) {
        this.priceRowEntityRepository = priceRowEntityRepository;
    }

    @Override
    public Optional<PriceRowEntity> findMatch(PriceRowMatchingContext context) {
        if (!context.hasRequiredFields()) {
            return Optional.empty();
        }

        Specification<PriceRowEntity> spec = buildSpecification(context);
        List<PriceRowEntity> candidates = priceRowEntityRepository.findAll(spec);

        // Group refs require exact-set matching – filter in Java after the DB query
        Set<String> normalizedGroupRefs = context.getGroupRefs() != null
                ? context.getGroupRefs() : new HashSet<>();

        for (PriceRowEntity candidate : candidates) {
            Set<String> candidateGroupRefs = candidate.getGroups() != null
                    ? candidate.getGroups().stream()
                            .map(GroupEntity::getId)
                            .collect(Collectors.toSet())
                    : new HashSet<>();

            if (normalizedGroupRefs.equals(candidateGroupRefs)) {
                return Optional.of(candidate);
            }
        }

        return Optional.empty();
    }

    /**
     * Builds a JPA {@link Specification} for all scalar and entity-reference fields.
     * Group membership is intentionally excluded because exact-set comparison requires
     * post-query filtering in Java.
     */
    private Specification<PriceRowEntity> buildSpecification(PriceRowMatchingContext context) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("pricedResourceId"), context.getPricedResourceId()));
            predicates.add(cb.equal(root.get("minQuantity"), context.getMinQuantity()));
            predicates.add(cb.equal(root.get("taxIncluded"), context.isTaxIncluded()));

            // Entity-reference fields: resolved via JOIN on natural key so that no external
            // service calls are needed. If the referenced entity does not exist the JOIN
            // simply produces no candidates.
            Join<PriceRowEntity, UnitEntity> unitJoin = root.join("unitRef", JoinType.INNER);
            predicates.add(cb.equal(unitJoin.get("symbol"), context.getUnitRef()));

            Join<PriceRowEntity, CurrencyEntity> currencyJoin = root.join("currencyRef", JoinType.INNER);
            predicates.add(cb.equal(currencyJoin.get("currencyKey"), context.getCurrencyRef()));

            Join<PriceRowEntity, TaxClassEntity> taxClassJoin = root.join("taxClassRef", JoinType.INNER);
            predicates.add(cb.equal(taxClassJoin.get("taxClassId"), context.getTaxClassRef()));

            // Nullable enum / date fields: NULL must match NULL
            if (context.getPriceType() != null) {
                predicates.add(cb.equal(root.get("priceType"), context.getPriceType()));
            } else {
                predicates.add(cb.isNull(root.get("priceType")));
            }

            if (context.getValidFrom() != null) {
                predicates.add(cb.equal(root.get("validFrom"), context.getValidFrom()));
            } else {
                predicates.add(cb.isNull(root.get("validFrom")));
            }

            if (context.getValidTo() != null) {
                predicates.add(cb.equal(root.get("validTo"), context.getValidTo()));
            } else {
                predicates.add(cb.isNull(root.get("validTo")));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
