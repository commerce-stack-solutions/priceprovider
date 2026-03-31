package io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.commercestacksolutions.commons.dataaccess.entity.AuditableEntity;
import io.commercestacksolutions.commons.dataaccess.meta.MetaMandatoryField;
import io.commercestacksolutions.priceproviderservice.dataaccess.country.entity.CountryEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "taxClassId")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TaxClassEntity implements AuditableEntity {
    @Id
        private String taxClassId;
    
    @Column(precision = 4, scale = 2)
    @MetaMandatoryField
    private BigDecimal taxRate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "country_iso_key", nullable = true)
    @JsonIgnoreProperties({"taxClasses", "hibernateLazyInitializer", "handler"})
    @MetaMandatoryField
    private CountryEntity countryRef;

    private OffsetDateTime createdAt;
    
    private OffsetDateTime lastModifiedAt;

    public TaxClassEntity() {
    }

    public TaxClassEntity(String taxClassId) {
        this.taxClassId = taxClassId;
    }

    public String getTaxClassId() {
        return taxClassId;
    }

    public void setTaxClassId(String taxClassId) {
        this.taxClassId = taxClassId;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public CountryEntity getCountry() {
        return countryRef;
    }

    public void setCountry(CountryEntity country) {
        this.countryRef = country;
    }

    @Transient
    public String getCountryRef() {
        return countryRef != null ? countryRef.getIsoKey() : null;
    }

    @Transient
    public void setCountryRef(String countryRef) {
        if (countryRef != null) {
            CountryEntity country = new CountryEntity();
            country.setIsoKey(countryRef);
            this.countryRef = country;
        } else {
            this.countryRef = null;
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
        return "TaxClassEntity{" +
                "taxClassId='" + taxClassId + '\'' +
                ", taxRate=" + taxRate +
                ", countryRef=" + (countryRef != null ? countryRef.getIsoKey() : null) +
                ", createdAt=" + createdAt +
                ", lastModifiedAt=" + lastModifiedAt +
                '}';
    }
}
