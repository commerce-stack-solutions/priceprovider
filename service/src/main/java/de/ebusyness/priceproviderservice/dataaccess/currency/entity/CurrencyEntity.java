package de.ebusyness.priceproviderservice.dataaccess.currency.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import de.ebusyness.commons.dataaccess.entity.AuditableEntity;
import de.ebusyness.commons.dataaccess.meta.MetaMandatoryField;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "currencyKey")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CurrencyEntity implements AuditableEntity {
    @Id
        private String currencyKey;
    
    @MetaMandatoryField
    private String symbol;
    
    private OffsetDateTime createdAt;
    
    private OffsetDateTime lastModifiedAt;
    
    // Localized name/language pairs
    @ElementCollection
    @CollectionTable(name = "currency_localized_names", joinColumns = @JoinColumn(name = "currency_key"))
    @MapKeyColumn(name = "language_code")
    @Column(name = "name")
    private Map<String, String> name;

    public CurrencyEntity() {
    }

    public CurrencyEntity(String currencyKey) {
        this.currencyKey = currencyKey;
    }

    public String getCurrencyKey() {
        return currencyKey;
    }

    public void setCurrencyKey(String currencyKey) {
        this.currencyKey = currencyKey;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Map<String, String> getName() {
        return name;
    }

    public void setName(Map<String, String> name) {
        this.name = name;
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
        return "CurrencyEntity{" +
                "currencyKey='" + currencyKey + '\'' +
                ", symbol='" + symbol + '\'' +
                ", name=" + name +
                ", createdAt=" + createdAt +
                ", lastModifiedAt=" + lastModifiedAt +
                '}';
    }
}
