package io.commercestacksolutions.priceproviderservice.dataaccess.language.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.commercestacksolutions.commons.dataaccess.entity.AuditableEntity;
import io.commercestacksolutions.commons.dataaccess.meta.MandatoryField;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.Map;

@Entity
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "isoKey")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class LanguageEntity implements AuditableEntity {
    @Id
        private String isoKey;
    
    @MandatoryField
    private Boolean active;
    
    private Boolean mandatory;
    
    private OffsetDateTime createdAt;
    
    private OffsetDateTime lastModifiedAt;
    
    // Localized name/language pairs
    @ElementCollection
    @CollectionTable(name = "language_localized_names", joinColumns = @JoinColumn(name = "iso_key"))
    @MapKeyColumn(name = "language_code")
    @Column(name = "name")
    private Map<String, String> name;

    public LanguageEntity() {
    }

    public LanguageEntity(String isoKey) {
        this.isoKey = isoKey;
    }

    public String getIsoKey() {
        return isoKey;
    }

    public void setIsoKey(String isoKey) {
        this.isoKey = isoKey;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getMandatory() {
        return mandatory;
    }

    public void setMandatory(Boolean mandatory) {
        this.mandatory = mandatory;
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
        return "LanguageEntity{" +
                "isoKey='" + isoKey + '\'' +
                ", active=" + active +
                ", mandatory=" + mandatory +
                ", name=" + name +
                ", createdAt=" + createdAt +
                ", lastModifiedAt=" + lastModifiedAt +
                '}';
    }
}
