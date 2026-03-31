package io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.commercestacksolutions.commons.dataaccess.entity.AuditableEntity;
import io.commercestacksolutions.commons.dataaccess.meta.MetaMandatoryField;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "symbol")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class UnitEntity implements AuditableEntity {
    @Id
    private String symbol;

    @ElementCollection
    @CollectionTable(name = "unit_localized_names", joinColumns = @JoinColumn(name = "symbol"))
    @MapKeyColumn(name = "language_code")
    @Column(name = "name")
    @MetaMandatoryField
    private Map<String, String> name;

    private String measure;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "base_unit_symbol")
    private UnitEntity baseUnitRef;

    @Column(precision = 19, scale = 9)
    private BigDecimal factor;
    
    private OffsetDateTime createdAt;
    
    private OffsetDateTime lastModifiedAt;

    public UnitEntity() {
    }

    public UnitEntity(String symbol) {
        this.symbol = symbol;
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

    public String getMeasure() {
        return measure;
    }

    public void setMeasure(String measure) {
        this.measure = measure;
    }

    public UnitEntity getBaseUnit() {
        return baseUnitRef;
    }

    public void setBaseUnit(UnitEntity baseUnit) {
        this.baseUnitRef = baseUnit;
    }

    @Transient
    public String getBaseUnitRef() {
        return baseUnitRef != null ? baseUnitRef.getSymbol() : null;
    }

    @Transient
    public void setBaseUnitRef(String baseUnitRef) {
        if (baseUnitRef != null) {
            UnitEntity baseUnit = new UnitEntity();
            baseUnit.setSymbol(baseUnitRef);
            this.baseUnitRef = baseUnit;
        } else {
            this.baseUnitRef = null;
        }
    }

    public BigDecimal getFactor() {
        return factor;
    }

    public void setFactor(BigDecimal factor) {
        this.factor = factor;
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
        String baseSymbol = null;
        try {
            if (this.baseUnitRef != null) {
                // Nur das Symbol der BaseUnit ausgeben (kein Rekursionsrisiko durch vollständiges baseUnitRef.toString())
                baseSymbol = this.baseUnitRef.getSymbol();
            }
        } catch (Exception e) {
            // Falls das Lazy-Proxy einen Fehler wirft oder ein anderes Problem auftritt, Fallback auf null
            baseSymbol = null;
        }
        return "UnitEntity{" +
                "symbol='" + symbol + '\'' +
                ", name=" + name +
                ", measure='" + measure + '\'' +
                ", baseUnitSymbol='" + baseSymbol + '\'' +
                ", factor=" + factor +
                ", createdAt=" + createdAt +
                ", lastModifiedAt=" + lastModifiedAt +
                '}';
    }
}