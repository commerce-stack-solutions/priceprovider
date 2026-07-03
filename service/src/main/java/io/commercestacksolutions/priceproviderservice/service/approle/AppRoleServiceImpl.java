package io.commercestacksolutions.priceproviderservice.service.approle;

import io.commercestacksolutions.commons.query.*;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.service.entity.authorization.EntityAuthorizationService;
import io.commercestacksolutions.commons.service.entity.validation.EntityValidator;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.AppPermissionEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.AppRoleEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppRoleEntity;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class AppRoleServiceImpl implements AppRoleService {

    private final AppRoleEntityRepository appRoleEntityRepository;
    private final AppPermissionEntityRepository appPermissionEntityRepository;
    private final EntityValidator<AppRoleEntity> entityValidator;
    private final QueryParser queryParser;
    private final AuthorizationContext authorizationContext;
    private final EntityAuthorizationService entityAuthorizationService;
    private final EntityManager entityManager;

    @Autowired
    public AppRoleServiceImpl(AppRoleEntityRepository appRoleEntityRepository,
                              AppPermissionEntityRepository appPermissionEntityRepository,
                              List<ValidationRule<AppRoleEntity>> validationRules,
                              AuthorizationContext authorizationContext,
                              EntityAuthorizationService entityAuthorizationService,
                              EntityManager entityManager) {
        this.appRoleEntityRepository = appRoleEntityRepository;
        this.appPermissionEntityRepository = appPermissionEntityRepository;
        this.entityValidator = new EntityValidator<>(validationRules);
        this.queryParser = new QueryParser(AppRoleEntity.class);
        this.authorizationContext = authorizationContext;
        this.entityAuthorizationService = entityAuthorizationService;
        this.entityManager = entityManager;
    }

    @Override
    public Class<AppRoleEntity> getTargetClass() {
        return AppRoleEntity.class;
    }

    @Override
    public EntityValidator<AppRoleEntity> getEntityValidator() {
        return entityValidator;
    }

    @Override
    public <ID> JpaRepository<AppRoleEntity, ID> getRepository() {
        @SuppressWarnings("unchecked")
        JpaRepository<AppRoleEntity, ID> repo = (JpaRepository<AppRoleEntity, ID>) appRoleEntityRepository;
        return repo;
    }

    @Override
    public EntityManager getEntityManager() {
        return entityManager;
    }

    @Override
    public EntityAuthorizationService getEntityAuthorizationService() {
        return entityAuthorizationService;
    }

    @Override
    public <ID> ID extractEntityId(AppRoleEntity entity) {
        @SuppressWarnings("unchecked")
        ID id = (ID) entity.getId();
        return id;
    }

    @Override
    public AppRoleEntity save(AppRoleEntity roleEntity) throws EntityValidationException {
        return performGenericSave(roleEntity);
    }

    @Override
    public void resolveRelatedReferences(AppRoleEntity roleEntity) {
        resolvePermissionRefs(roleEntity);
    }

    /**
     * Resolves permission references by name or ID to ensure they are managed entities.
     * This prevents detached entity issues when saving roles.
     *
     * <p>When this method is called inside a long-running transaction (e.g. from
     * {@code loadDataAsync}), the {@code roleEntity} may already be a managed JPA
     * entity.  Setting transient {@code AppPermissionEntity} stubs on it and then
     * executing a JPQL query would trigger Hibernate's auto-flush, which in turn
     * throws a {@code TransientObjectException} because the stubs have no
     * database identity yet.</p>
     *
     * <p>The fix: collect the names/IDs from the stub objects first, then
     * <em>clear</em> the permission set on the entity before running any query.
     * With an empty (but valid) collection the auto-flush succeeds, and we
     * repopulate the collection with the fully-managed entities afterwards.</p>
     */
    private void resolvePermissionRefs(AppRoleEntity roleEntity) {
        if (roleEntity.getPermissionRefs() == null || roleEntity.getPermissionRefs().isEmpty()) {
            return;
        }

        // 1. Capture names / IDs from possibly-transient stub objects.
        List<String> names = new ArrayList<>();
        List<Long> ids = new ArrayList<>();
        for (AppPermissionEntity permRef : roleEntity.getPermissionRefs()) {
            if (permRef.getName() != null) {
                names.add(permRef.getName());
            } else if (permRef.getId() != null) {
                ids.add(permRef.getId());
            }
        }

        // 2. Remove the transient stubs from the entity BEFORE any query is
        //    executed.  This prevents Hibernate auto-flush from encountering
        //    unsaved transient instances when the entity is already managed.
        roleEntity.setPermissionRefs(new HashSet<>());

        // 3. Look up the real, managed entities and collect them.
        Set<AppPermissionEntity> managedPermissions = new HashSet<>();
        for (String name : names) {
            appPermissionEntityRepository.findByName(name)
                .ifPresent(managedPermissions::add);
        }
        for (Long id : ids) {
            appPermissionEntityRepository.findById(id)
                .ifPresent(managedPermissions::add);
        }

        // 4. Re-assign only managed entities to the role.
        roleEntity.setPermissionRefs(managedPermissions);
    }

    @Override
    public List<AppRoleEntity> getAllAppRoles() {
        return appRoleEntityRepository.findAll();
    }

    @Override
    public Page<AppRoleEntity> getAppRoles(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException {
        PageRequest pageRequest;
        if (sortBy != null && !sortBy.isEmpty()) {
            Sort.Direction direction = "desc".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
            List<Sort.Order> orders = new ArrayList<>();
            for (String field : sortBy) {
                orders.add(new Sort.Order(direction, field));
            }
            pageRequest = PageRequest.of(page, pageSize, Sort.by(orders));
        } else {
            pageRequest = PageRequest.of(page, pageSize);
        }

        if (query != null && !query.trim().isEmpty()) {
            QueryExpression expression = queryParser.parse(query);
            Specification<AppRoleEntity> spec = SpecificationBuilder.build(expression);
            return appRoleEntityRepository.findAll(spec, pageRequest);
        }

        return appRoleEntityRepository.findAll(pageRequest);
    }

    @Override
    public Optional<AppRoleEntity> getAppRoleById(Long id) {
        return appRoleEntityRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public AppRoleEntity getAppRole(Long id) {
        // Use repository-level fetch join to load permissions eagerly for this lookup
        AppRoleEntity role = appRoleEntityRepository.findByIdWithPermissions(id).orElse(null);
        // Ensure the permissionRefs collection is initialized while still in transaction
        if (role != null && role.getPermissionRefs() != null) {
            role.getPermissionRefs().size();
        }
        return role;
    }

    @Override
    public Optional<AppRoleEntity> getAppRoleByName(String name) {
        return appRoleEntityRepository.findByName(name);
    }

    @Override
    @Transactional(readOnly = true)
    public AppRoleEntity getAppRoleWithPermissionsByName(String name) {
        AppRoleEntity role = appRoleEntityRepository.findByNameWithPermissions(name).orElse(null);
        if (role != null && role.getPermissionRefs() != null) {
            role.getPermissionRefs().size();
        }
        return role;
    }

    @Override
    public AppRoleEntity updateAppRole(AppRoleEntity entity) throws EntityValidationException {
        return save(entity);
    }

    @Override
    public void deleteAppRole(Long id) {
        appRoleEntityRepository.findById(id).ifPresent(entity -> {
            // Check delete permission on the existing entity (before deletion)
            entityAuthorizationService.checkAccessBeforeAndAfter(
                entity,
                null,  // No "after" state for delete
                getEntityTypeName(),
                "delete",
                id.toString()
            );
            appRoleEntityRepository.deleteById(id);
        });
    }
}


