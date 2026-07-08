package io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import io.commercestacksolutions.commons.dataaccess.entity.AuditableEntity;
import io.commercestacksolutions.commons.dataaccess.meta.MandatoryField;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id", scope = AppRoleEntity.class)
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AppRoleEntity implements AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @MandatoryField
    @Column(unique = true, nullable = false)
    private String name;

    private String description;

    @ManyToMany
    @JoinTable(
        name = "approle_permissions",
        joinColumns = @JoinColumn(name = "approle_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    @JsonIdentityReference(alwaysAsId = true)
    private Set<AppPermissionEntity> permissionRefs = new HashSet<>();

    private OffsetDateTime createdAt;

    private OffsetDateTime lastModifiedAt;

    public AppRoleEntity() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Set<AppPermissionEntity> getPermissionRefs() {
        return permissionRefs;
    }

    public void setPermissionRefs(Set<AppPermissionEntity> permissionRefs) {
        this.permissionRefs = permissionRefs;
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
        return "AppRoleEntity{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", lastModifiedAt=" + lastModifiedAt +
                '}';
    }
}
