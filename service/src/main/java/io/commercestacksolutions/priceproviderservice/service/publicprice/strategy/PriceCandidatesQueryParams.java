package io.commercestacksolutions.priceproviderservice.service.publicprice.strategy;

import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.enums.PriceType;
import io.commercestacksolutions.priceproviderservice.service.group.model.GroupWithDistance;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Parameter object encapsulating all criteria for price candidate queries.
 *
 * This object is passed to {@link AbstractPermissionAwarePriceCandidatesQueryStrategy}
 * and provides a structured way to pass query parameters without having a long
 * list of individual parameters.
 *
 * <p>All fields are immutable after construction to ensure thread-safety.</p>
 */
public class PriceCandidatesQueryParams {

    private final String pricedResourceId;
    private final String currencyRef;
    private final PriceType priceType;
    private final String unitRef;
    private final BigDecimal quantity;
    private final OffsetDateTime referenceDate;
    private final boolean hasGroups;
    private final List<GroupWithDistance> groupHierarchy;
    private final String channelId;
    private final String countryKey;
    private final Boolean taxIncludedFilter;

    /**
     * Private constructor - use the Builder to create instances.
     */
    private PriceCandidatesQueryParams(Builder builder) {
        this.pricedResourceId = builder.pricedResourceId;
        this.currencyRef = builder.currencyRef;
        this.priceType = builder.priceType;
        this.unitRef = builder.unitRef;
        this.quantity = builder.quantity;
        this.referenceDate = builder.referenceDate;
        this.hasGroups = builder.hasGroups;
        this.groupHierarchy = builder.groupHierarchy;
        this.channelId = builder.channelId;
        this.countryKey = builder.countryKey;
        this.taxIncludedFilter = builder.taxIncludedFilter;
    }

    // Getters

    public String getPricedResourceId() {
        return pricedResourceId;
    }

    public String getCurrencyRef() {
        return currencyRef;
    }

    public PriceType getPriceType() {
        return priceType;
    }

    public String getUnitRef() {
        return unitRef;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public OffsetDateTime getReferenceDate() {
        return referenceDate;
    }

    public boolean isHasGroups() {
        return hasGroups;
    }

    public List<GroupWithDistance> getGroupHierarchy() {
        return groupHierarchy;
    }

    public String getChannelId() {
        return channelId;
    }

    public String getCountryKey() {
        return countryKey;
    }

    public Boolean getTaxIncludedFilter() {
        return taxIncludedFilter;
    }

    /**
     * Creates a new builder for PriceCandidatesQueryParams.
     *
     * @return A new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for PriceCandidatesQueryParams.
     *
     * Example usage:
     * <pre>{@code
     * PriceCandidatesQueryParams params = PriceCandidatesQueryParams.builder()
     *     .pricedResourceId("PROD-001")
     *     .currencyRef("EUR")
     *     .priceType(PriceType.SALES_PRICE)
     *     .quantity(new BigDecimal("10"))
     *     .referenceDate(OffsetDateTime.now())
     *     .build();
     * }</pre>
     */
    public static class Builder {
        private String pricedResourceId;
        private String currencyRef;
        private PriceType priceType;
        private String unitRef;
        private BigDecimal quantity;
        private OffsetDateTime referenceDate;
        private boolean hasGroups;
        private List<GroupWithDistance> groupHierarchy;
        private String channelId;
        private String countryKey;
        private Boolean taxIncludedFilter;

        private Builder() {
        }

        public Builder pricedResourceId(String pricedResourceId) {
            this.pricedResourceId = pricedResourceId;
            return this;
        }

        public Builder currencyRef(String currencyRef) {
            this.currencyRef = currencyRef;
            return this;
        }

        public Builder priceType(PriceType priceType) {
            this.priceType = priceType;
            return this;
        }

        public Builder unitRef(String unitRef) {
            this.unitRef = unitRef;
            return this;
        }

        public Builder quantity(BigDecimal quantity) {
            this.quantity = quantity;
            return this;
        }

        public Builder referenceDate(OffsetDateTime referenceDate) {
            this.referenceDate = referenceDate;
            return this;
        }

        public Builder hasGroups(boolean hasGroups) {
            this.hasGroups = hasGroups;
            return this;
        }

        public Builder groupHierarchy(List<GroupWithDistance> groupHierarchy) {
            this.groupHierarchy = groupHierarchy;
            return this;
        }

        public Builder channelId(String channelId) {
            this.channelId = channelId;
            return this;
        }

        public Builder countryKey(String countryKey) {
            this.countryKey = countryKey;
            return this;
        }

        public Builder taxIncludedFilter(Boolean taxIncludedFilter) {
            this.taxIncludedFilter = taxIncludedFilter;
            return this;
        }

        public PriceCandidatesQueryParams build() {
            return new PriceCandidatesQueryParams(this);
        }
    }
}
