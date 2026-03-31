package io.commercestacksolutions.priceproviderservice.dataaccess.channel.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.commercestacksolutions.commons.dataaccess.entity.AuditableEntity;
import io.commercestacksolutions.commons.dataaccess.meta.MetaDynamicEnum;
import io.commercestacksolutions.commons.dataaccess.meta.MetaMandatoryField;
import io.commercestacksolutions.priceproviderservice.dataaccess.country.entity.CountryEntity;
import io.commercestacksolutions.priceproviderservice.service.publicprice.strategy.PriceRepresentationMode;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChannelEntity implements AuditableEntity {

    @Id
        private String id;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "channel_countries",
            joinColumns = @JoinColumn(name = "channel_id"),
            inverseJoinColumns = @JoinColumn(name = "country_iso_key")
    )
    private Set<CountryEntity> allowedCountryRefs = new HashSet<>();

    @MetaDynamicEnum(beanType = PriceRepresentationMode.class)
    @MetaMandatoryField
    private String priceRepresentationMode;

    private OffsetDateTime createdAt;

    private OffsetDateTime lastModifiedAt;

    public ChannelEntity() {
    }

    public ChannelEntity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Set<CountryEntity> getAllowedCountryRefs() {
        return allowedCountryRefs;
    }

    public void setAllowedCountryRefs(Set<CountryEntity> allowedCountryRefs) {
        this.allowedCountryRefs = allowedCountryRefs;
    }

    public String getPriceRepresentationMode() {
        return priceRepresentationMode;
    }

    public void setPriceRepresentationMode(String priceRepresentationMode) {
        this.priceRepresentationMode = priceRepresentationMode;
    }

    @Transient
    public Set<String> getCountryRefs() {
        return allowedCountryRefs != null ? allowedCountryRefs.stream()
                .map(CountryEntity::getIsoKey)
                .collect(Collectors.toSet()) : new HashSet<>();
    }

    @Transient
    public void setCountryRefs(Set<String> countryRefs) {
        if (countryRefs != null && !countryRefs.isEmpty()) {
            this.allowedCountryRefs = countryRefs.stream()
                    .map(ref -> {
                        CountryEntity country = new CountryEntity();
                        country.setIsoKey(ref);
                        return country;
                    })
                    .collect(Collectors.toSet());
        } else {
            this.allowedCountryRefs = new HashSet<>();
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
        return "ChannelEntity{" +
                "id='" + id + '\'' +
                ", allowedCountryRefs=" + (allowedCountryRefs != null ? allowedCountryRefs.stream().map(CountryEntity::getIsoKey).collect(Collectors.toSet()) : null) +
                ", priceRepresentationMode='" + priceRepresentationMode + '\'' +
                ", createdAt=" + createdAt +
                ", lastModifiedAt=" + lastModifiedAt +
                '}';
    }
}
