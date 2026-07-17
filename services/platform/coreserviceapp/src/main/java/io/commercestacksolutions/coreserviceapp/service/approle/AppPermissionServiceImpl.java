package io.commercestacksolutions.coreserviceapp.service.approle;

import io.commercestacksolutions.commons.query.*;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.service.entity.authorization.EntityAuthorizationService;
import io.commercestacksolutions.commons.service.entity.validation.EntityValidator;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.config.security.AuthorizationContext;
import io.commercestacksolutions.coreserviceapp.dataaccess.approle.AppPermissionEntityRepository;
import io.commercestacksolutions.coreserviceapp.dataaccess.approle.entity.CommonAppPermission;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AppPermissionServiceImpl implements AppPermissionService {

    private static final Logger logger = LoggerFactory.getLogger(AppPermissionServiceImpl.class);

    private final AppPermissionEntityRepository appPermissionEntityRepository;
    private final EntityValidator<CommonAppPermission> entityValidator;
    private final QueryParser queryParser;
    private final AuthorizationContext authorizationContext;
    private final EntityAuthorizationService entityAuthorizationService;
    private final EntityManager entityManager;

    @Autowired
    public AppPermissionServiceImpl(AppPermissionEntityRepository appPermissionEntityRepository,
                                    List<ValidationRule<CommonAppPermission>> validationRules,
                                    AuthorizationContext authorizationContext,
                                    EntityAuthorizationService entityAuthorizationService,
                                    EntityManager entityManager) {
        this.appPermissionEntityRepository = appPermissionEntityRepository;
        this.entityValidator = new EntityValidator<>(validationRules);
        this.queryParser = new QueryParser(CommonAppPermission.class);
        this.authorizationContext = authorizationContext;
        this.entityAuthorizationService = entityAuthorizationService;
        this.entityManager = entityManager;
    }

    @Override
    public Class<CommonAppPermission> getTargetClass() {
        return CommonAppPermission.class;
    }

    @Override
    public EntityValidator<CommonAppPermission> getEntityValidator() {
        return entityValidator;
    }

    @Override
    public <ID> JpaRepository<CommonAppPermission, ID> getRepository() {
        @SuppressWarnings("unchecked")
        JpaRepository<CommonAppPermission, ID> repo = (JpaRepository<CommonAppPermission, ID>) appPermissionEntityRepository;
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
    public <ID> ID extractEntityId(CommonAppPermission entity) {
        @SuppressWarnings("unchecked")
        ID id = (ID) entity.getId();
        return id;
    }

    @Override
    public CommonAppPermission save(CommonAppPermission permissionEntity) throws EntityValidationException {
        return performGenericSave(permissionEntity);
    }

    @Override
    public CommonAppPermission createPermission(String name, String description) {
        CommonAppPermission permission = new CommonAppPermission();
        permission.setName(name);
        permission.setDescription(description);
        try {
            return save(permission);
        } catch (EntityValidationException e) {
            throw new IllegalStateException("Failed to create app permission: " + name, e);
        }
    }

    @Override
    public List<CommonAppPermission> getAllAppPermissions() {
        return appPermissionEntityRepository.findAll();
    }

    @Override
    public Page<CommonAppPermission> getAppPermissions(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException {
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
            Specification<CommonAppPermission> spec = SpecificationBuilder.build(expression);
            return appPermissionEntityRepository.findAll(spec, pageRequest);
        }

        return appPermissionEntityRepository.findAll(pageRequest);
    }

    @Override
    public Optional<CommonAppPermission> getAppPermissionById(Long id) {
        return appPermissionEntityRepository.findById(id);
    }

    @Override
    public CommonAppPermission getAppPermission(Long id) {
        return appPermissionEntityRepository.findById(id).orElse(null);
    }

    @Override
    public Optional<CommonAppPermission> getAppPermissionByName(String name) {
        return appPermissionEntityRepository.findByName(name);
    }

    @Override
    public CommonAppPermission updateAppPermission(CommonAppPermission entity) throws EntityValidationException {
        return save(entity);
    }

    @Override
    public void deleteAppPermission(Long id) {
        appPermissionEntityRepository.findById(id).ifPresent(entity -> {
            // Check delete permission on the existing entity (before deletion)
            entityAuthorizationService.checkAccessBeforeAndAfter(
                entity,
                null,  // No "after" state for delete
                getEntityTypeName(),
                "delete",
                id.toString()
            );
            appPermissionEntityRepository.deleteById(id);
        });
    }
}
