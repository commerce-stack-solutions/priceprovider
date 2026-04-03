package io.commercestacksolutions.priceproviderservice.dataaccess.group.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import io.commercestacksolutions.commons.dataaccess.entity.AuditableEntity;
import io.commercestacksolutions.commons.dataaccess.meta.MetaMandatoryField;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "path", scope = GroupEntity.class)
// Scope auf die konkrete Entity setzen, damit verschiedene Entity-Typen nicht dieselben Object-IDs im globalen Object-Scope teilen
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class GroupEntity implements AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @MetaMandatoryField
    @Column(unique = true, nullable = false)
    private String path;

    @MetaMandatoryField
    private String name;

    @ManyToMany
    @JoinTable(
        name = "group_parents",
        joinColumns = @JoinColumn(name = "group_id"),
        inverseJoinColumns = @JoinColumn(name = "parent_id")
    )
    // Parents als Path-Referenzen (z.B. { "path": "GRP-SAMPLE-001" }) lesen/schreiben
    @JsonIdentityReference(alwaysAsId = true)
    private Set<GroupEntity> parentRefs = new HashSet<>();
    
    @ManyToMany(mappedBy = "parentRefs")
    // Subs ebenfalls als Path-Referenzen serialisieren
    @JsonIdentityReference(alwaysAsId = true)
    private Set<GroupEntity> subRefs = new HashSet<>();

    private OffsetDateTime createdAt;
    
    private OffsetDateTime lastModifiedAt;

    public GroupEntity() {
    }

    public GroupEntity(String path) {
        this.path = path;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<GroupEntity> getParentRefs() {
        return parentRefs;
    }

    public void setParentRefs(Set<GroupEntity> parentRefs) {
        this.parentRefs = parentRefs;
    }

    public Set<GroupEntity> getSubRefs() {
        return subRefs;
    }

    public void setSubRefs(Set<GroupEntity> subRefs) {
        this.subRefs = subRefs;
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
        return "GroupEntity{" +
                "id=" + id +
                ", path='" + path + '\'' +
                ", name='" + name + '\'' +
                ", createdAt=" + createdAt +
                ", lastModifiedAt=" + lastModifiedAt +
                '}';
    }
}
