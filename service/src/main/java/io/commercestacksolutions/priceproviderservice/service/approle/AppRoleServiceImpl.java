package io.commercestacksolutions.priceproviderservice.service.approle;

import io.commercestacksolutions.commons.query.*;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.service.entity.validation.EntityValidator;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.AppPermissionEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.AppRoleEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppRoleEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
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

    @Autowired
    public AppRoleServiceImpl(AppRoleEntityRepository appRoleEntityRepository,
                              AppPermissionEntityRepository appPermissionEntityRepository,
                              List<ValidationRule<AppRoleEntity>> validationRules) {
        this.appRoleEntityRepository = appRoleEntityRepository;
        this.appPermissionEntityRepository = appPermissionEntityRepository;
        this.entityValidator = new EntityValidator<>(validationRules);
        this.queryParser = new QueryParser(AppRoleEntity.class);
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
    public AppRoleEntity save(AppRoleEntity roleEntity) throws EntityValidationException {
        // Resolve permission entities by name to avoid detached entity issues
        if (roleEntity.getPermissionRefs() != null && !roleEntity.getPermissionRefs().isEmpty()) {
            Set<AppPermissionEntity> managedPermissions = new HashSet<>();
            for (AppPermissionEntity permRef : roleEntity.getPermissionRefs()) {
                if (permRef.getName() != null) {
                    appPermissionEntityRepository.findByName(permRef.getName())
                        .ifPresent(managedPermissions::add);
                } else if (permRef.getId() != null) {
                    appPermissionEntityRepository.findById(permRef.getId())
                        .ifPresent(managedPermissions::add);
                }
            }
            roleEntity.setPermissionRefs(managedPermissions);
        }
        validateEntity(roleEntity);
        updateAuditTimestamps(roleEntity);
        return appRoleEntityRepository.save(roleEntity);
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
        appRoleEntityRepository.deleteById(id);
    }
}


