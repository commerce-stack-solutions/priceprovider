package io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.commercestacksolutions.commons.dataaccess.entity.AuditableEntity;
import io.commercestacksolutions.commons.dataaccess.meta.MandatoryField;
import io.commercestacksolutions.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.enums.PriceType;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import jakarta.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PriceRowEntity implements AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
    @MandatoryField
    private String pricedResourceId;
    @Column(precision = 19, scale = 2)
    @MandatoryField
    private BigDecimal priceValue;
    @Column(precision = 19, scale = 2)
    @MandatoryField
    private BigDecimal minQuantity;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_symbol")
    @MandatoryField
    private UnitEntity unitRef;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "currency_key")
    @MandatoryField
    private CurrencyEntity currencyRef;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tax_class_id", nullable = true)
    @MandatoryField
    private TaxClassEntity taxClassRef;
    @Enumerated(EnumType.STRING)
    private PriceType priceType;
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private OffsetDateTime validFrom;
    private OffsetDateTime validTo;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "price_row_groups",
        joinColumns = @JoinColumn(name = "price_row_id"),
        inverseJoinColumns = @JoinColumn(name = "group_id")
    )
    private Set<GroupEntity> groupRefs = new HashSet<>();
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "price_row_channels",
        joinColumns = @JoinColumn(name = "price_row_id"),
        inverseJoinColumns = @JoinColumn(name = "channel_id")
    )
    private Set<ChannelEntity> channelRefs = new HashSet<>();
    private boolean taxIncluded;
    private OffsetDateTime createdAt;
    private OffsetDateTime lastModifiedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isTaxIncluded() {
        return taxIncluded;
    }

    public void setTaxIncluded(boolean taxIncluded) {
        this.taxIncluded = taxIncluded;
    }

    public String getPricedResourceId() {
        return pricedResourceId;
    }

    public void setPricedResourceId(String pricedResourceId) {
        this.pricedResourceId = pricedResourceId;
    }

    public BigDecimal getMinQuantity() {
        return minQuantity;
    }

    public void setMinQuantity(BigDecimal minQuantity) {
        this.minQuantity = minQuantity;
    }

    public UnitEntity getUnit() {
        return unitRef;
    }

    public void setUnit(UnitEntity unit) {
        this.unitRef = unit;
    }

    public BigDecimal getPriceValue() {
        return priceValue;
    }

    public void setPriceValue(BigDecimal priceValue) {
        this.priceValue = priceValue;
    }

    public CurrencyEntity getCurrency() {
        return currencyRef;
    }

    public void setCurrency(CurrencyEntity currency) {
        this.currencyRef = currency;
    }

    public TaxClassEntity getTaxClass() {
        return taxClassRef;
    }

    public void setTaxClass(TaxClassEntity taxClass) {
        this.taxClassRef = taxClass;
    }

    public PriceType getPriceType() {
        return priceType;
    }

    public void setPriceType(PriceType priceType) {
        this.priceType = priceType;
    }

    public OffsetDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(OffsetDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public OffsetDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(OffsetDateTime validTo) {
        this.validTo = validTo;
    }

    public Set<GroupEntity> getGroups() {
        return groupRefs;
    }

    public void setGroups(Set<GroupEntity> groups) {
        this.groupRefs = groups;
    }

    @Transient
    public String getUnitRef() {
        return unitRef != null ? unitRef.getSymbol() : null;
    }

    @Transient
    public void setUnitRef(String unitRef) {
        if (unitRef != null) {
            UnitEntity unit = new UnitEntity();
            unit.setSymbol(unitRef);
            this.unitRef = unit;
        } else {
            this.unitRef = null;
        }
    }

    @Transient
    public String getCurrencyRef() {
        return currencyRef != null ? currencyRef.getCurrencyKey() : null;
    }

    @Transient
    public void setCurrencyRef(String currencyRef) {
        if (currencyRef != null) {
            CurrencyEntity currency = new CurrencyEntity();
            currency.setCurrencyKey(currencyRef);
            this.currencyRef = currency;
        } else {
            this.currencyRef = null;
        }
    }

    @Transient
    public String getTaxClassRef() {
        return taxClassRef != null ? taxClassRef.getTaxClassId() : null;
    }

    @Transient
    public void setTaxClassRef(String taxClassRef) {
        if (taxClassRef != null) {
            TaxClassEntity taxClass = new TaxClassEntity();
            taxClass.setTaxClassId(taxClassRef);
            this.taxClassRef = taxClass;
        } else {
            this.taxClassRef = null;
        }
    }

    @Transient
    public Set<String> getGroupRefs() {
        return groupRefs != null ? groupRefs.stream()
                .filter(g -> g != null && g.getPath() != null)
                .map(GroupEntity::getPath)
                .collect(Collectors.toSet()) : new HashSet<>();
    }

    @Transient
    public void setGroupRefs(Set<String> groupRefs) {
        if (groupRefs != null && !groupRefs.isEmpty()) {
            this.groupRefs = groupRefs.stream()
                    .map(ref -> {
                        GroupEntity group = new GroupEntity();
                        group.setPath(ref);
                        return group;
                    })
                    .collect(Collectors.toSet());
        } else {
            this.groupRefs = new HashSet<>();
        }
    }

    public Set<ChannelEntity> getChannels() {
        return channelRefs;
    }

    public void setChannels(Set<ChannelEntity> channels) {
        this.channelRefs = channels;
    }

    @Transient
    public Set<String> getChannelRefs() {
        return channelRefs != null ? channelRefs.stream()
                .map(ChannelEntity::getId)
                .collect(Collectors.toSet()) : new HashSet<>();
    }

    @Transient
    public void setChannelRefs(Set<String> channelRefs) {
        if (channelRefs != null && !channelRefs.isEmpty()) {
            this.channelRefs = channelRefs.stream()
                    .map(ref -> {
                        ChannelEntity channel = new ChannelEntity();
                        channel.setId(ref);
                        return channel;
                    })
                    .collect(Collectors.toSet());
        } else {
            this.channelRefs = new HashSet<>();
        }
    }

    @Override
    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public OffsetDateTime getLastModifiedAt() {
        return lastModifiedAt;
    }

    @Override
    public void setLastModifiedAt(OffsetDateTime lastModifiedAt) {
        this.lastModifiedAt = lastModifiedAt;
    }

    @Override
    public String toString() {
        return "PriceRowEntity{" +
                "id=" + id +
                ", pricedResourceId='" + pricedResourceId + '\'' +
                ", priceValue=" + priceValue +
                ", minQuantity=" + minQuantity +
                ", unitRef=" + (unitRef != null ? unitRef.getSymbol() : null) +
                ", currencyRef=" + (currencyRef != null ? currencyRef.getCurrencyKey() : null) +
                ", taxClassRef=" + (taxClassRef != null ? taxClassRef.getTaxClassId() : null) +
                ", priceType=" + priceType +
                ", validFrom=" + validFrom +
                ", validTo=" + validTo +
                ", groupRefs=" + (groupRefs != null ? groupRefs.stream().map(GroupEntity::getPath).collect(Collectors.toSet()) : null) +
                ", channelRefs=" + (channelRefs != null ? channelRefs.stream().map(ChannelEntity::getId).collect(Collectors.toSet()) : null) +
                ", taxIncluded=" + taxIncluded +
                ", createdAt=" + createdAt +
                ", lastModifiedAt=" + lastModifiedAt +
                '}';
    }
}