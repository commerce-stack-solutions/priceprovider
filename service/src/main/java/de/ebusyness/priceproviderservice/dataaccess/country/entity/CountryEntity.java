package de.ebusyness.priceproviderservice.dataaccess.country.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import de.ebusyness.commons.dataaccess.entity.AuditableEntity;
import de.ebusyness.commons.dataaccess.meta.MetaMandatoryField;
import de.ebusyness.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import de.ebusyness.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "isoKey")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CountryEntity implements AuditableEntity {

    @Id
        private String isoKey;

    @ElementCollection
    @CollectionTable(name = "country_localized_names", joinColumns = @JoinColumn(name = "iso_key"))
    @MapKeyColumn(name = "language_code")
    @Column(name = "name")
    @MetaMandatoryField
    private Map<String, String> name;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "country_currencies",
            joinColumns = @JoinColumn(name = "country_iso_key"),
            inverseJoinColumns = @JoinColumn(name = "currency_key")
    )
    private Set<CurrencyEntity> allowedCurrencyRefs = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_currency_key")
    @MetaMandatoryField
    private CurrencyEntity primaryCurrencyRef;

    @OneToMany(mappedBy = "countryRef", fetch = FetchType.LAZY)
    @JsonIgnoreProperties("countryRef")
    private List<TaxClassEntity> taxClassRefs = new ArrayList<>();

    private OffsetDateTime createdAt;

    private OffsetDateTime lastModifiedAt;

    public CountryEntity() {
    }

    public CountryEntity(String isoKey) {
        this.isoKey = isoKey;
    }

    public String getIsoKey() {
        return isoKey;
    }

    public void setIsoKey(String isoKey) {
        this.isoKey = isoKey;
    }

    public Map<String, String> getName() {
        return name;
    }

    public void setName(Map<String, String> name) {
        this.name = name;
    }

    public List<TaxClassEntity> getTaxClassRefs() {
        return taxClassRefs;
    }

    public void setTaxClassRefs(List<TaxClassEntity> taxClassRefs) {
        this.taxClassRefs = taxClassRefs;
    }

    public Set<CurrencyEntity> getAllowedCurrencyRefs() {
        return allowedCurrencyRefs;
    }

    public void setAllowedCurrencyRefs(Set<CurrencyEntity> allowedCurrencyRefs) {
        this.allowedCurrencyRefs = allowedCurrencyRefs;
    }

    public CurrencyEntity getPrimaryCurrencyRef() {
        return primaryCurrencyRef;
    }

    public void setPrimaryCurrencyRef(CurrencyEntity primaryCurrencyRef) {
        this.primaryCurrencyRef = primaryCurrencyRef;
    }

    @Transient
    public Set<String> getCurrencyRefs() {
        return allowedCurrencyRefs != null ? allowedCurrencyRefs.stream()
                .map(CurrencyEntity::getCurrencyKey)
                .collect(Collectors.toSet()) : new HashSet<>();
    }

    @Transient
    public void setCurrencyRefs(Set<String> currencyRefs) {
        if (currencyRefs != null && !currencyRefs.isEmpty()) {
            this.allowedCurrencyRefs = currencyRefs.stream()
                    .map(ref -> {
                        CurrencyEntity currency = new CurrencyEntity();
                        currency.setCurrencyKey(ref);
                        return currency;
                    })
                    .collect(Collectors.toSet());
        } else {
            this.allowedCurrencyRefs = new HashSet<>();
        }
    }

    @Transient
    public String getPrimaryCurrencyKey() {
        return primaryCurrencyRef != null ? primaryCurrencyRef.getCurrencyKey() : null;
    }

    @Transient
    public void setPrimaryCurrencyKey(String primaryCurrencyKey) {
        if (primaryCurrencyKey != null && !primaryCurrencyKey.isBlank()) {
            CurrencyEntity currency = new CurrencyEntity();
            currency.setCurrencyKey(primaryCurrencyKey);
            this.primaryCurrencyRef = currency;
        } else {
            this.primaryCurrencyRef = null;
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
        return "CountryEntity{" +
                "isoKey='" + isoKey + '\'' +
                ", name=" + name +
                ", allowedCurrencyRefs=" + (allowedCurrencyRefs != null ? allowedCurrencyRefs.stream().map(CurrencyEntity::getCurrencyKey).collect(Collectors.toSet()) : null) +
                ", primaryCurrencyRef=" + (primaryCurrencyRef != null ? primaryCurrencyRef.getCurrencyKey() : null) +
                ", createdAt=" + createdAt +
                ", lastModifiedAt=" + lastModifiedAt +
                '}';
    }
}
