package de.ebusyness.priceproviderservice.dataaccess.pricerow;

import de.ebusyness.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.enums.PriceType;
import de.ebusyness.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import de.ebusyness.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface PriceRowEntityRepository extends JpaRepository<PriceRowEntity, Long>, JpaSpecificationExecutor<PriceRowEntity> {
    
    /**
     * Finds a price row by matching key fields (excluding price value).
     * This is used for bulk create-or-update operations to determine if a price row should be updated.
     * Note: Group matching is handled separately in the service layer due to collection comparison complexity.
     * 
     * @param pricedResourceId the priced resource identifier
     * @param minQuantity the minimum quantity
     * @param unit the unit entity
     * @param currency the currency entity
     * @param taxClass the tax class entity
     * @param taxIncluded whether tax is included
     * @param priceType the price type
     * @param validFrom the valid from date
     * @param validTo the valid to date
     * @return list of matching price rows (may need further filtering for groups)
     */
    @Query("SELECT p FROM PriceRowEntity p WHERE p.pricedResourceId = :pricedResourceId " +
           "AND p.minQuantity = :minQuantity " +
           "AND p.unitRef = :unit " +
           "AND p.currencyRef = :currency " +
           "AND p.taxClassRef = :taxClass " +
           "AND p.taxIncluded = :taxIncluded " +
           "AND (:priceType IS NULL AND p.priceType IS NULL OR p.priceType = :priceType) " +
           "AND (:validFrom IS NULL AND p.validFrom IS NULL OR p.validFrom = :validFrom) " +
           "AND (:validTo IS NULL AND p.validTo IS NULL OR p.validTo = :validTo)")
    List<PriceRowEntity> findByMatchingFields(
        @Param("pricedResourceId") String pricedResourceId,
        @Param("minQuantity") BigDecimal minQuantity,
        @Param("unit") UnitEntity unit,
        @Param("currency") CurrencyEntity currency,
        @Param("taxClass") TaxClassEntity taxClass,
        @Param("taxIncluded") boolean taxIncluded,
        @Param("priceType") PriceType priceType,
        @Param("validFrom") OffsetDateTime validFrom,
        @Param("validTo") OffsetDateTime validTo
    );
}