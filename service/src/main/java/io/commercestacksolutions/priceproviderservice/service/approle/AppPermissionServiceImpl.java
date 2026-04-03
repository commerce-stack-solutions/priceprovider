package io.commercestacksolutions.priceproviderservice.service.approle;

import io.commercestacksolutions.commons.query.*;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.service.entity.validation.EntityValidator;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.AppPermissionEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AppPermissionServiceImpl implements AppPermissionService {

    private static final Logger logger = LoggerFactory.getLogger(AppPermissionServiceImpl.class);

    private final AppPermissionEntityRepository appPermissionEntityRepository;
    private final EntityValidator<AppPermissionEntity> entityValidator;
    private final QueryParser queryParser;

    @Autowired
    public AppPermissionServiceImpl(AppPermissionEntityRepository appPermissionEntityRepository,
                                    List<ValidationRule<AppPermissionEntity>> validationRules) {
        this.appPermissionEntityRepository = appPermissionEntityRepository;
        this.entityValidator = new EntityValidator<>(validationRules);
        this.queryParser = new QueryParser(AppPermissionEntity.class);
    }

    @Override
    public Class<AppPermissionEntity> getTargetClass() {
        return AppPermissionEntity.class;
    }

    @Override
    public EntityValidator<AppPermissionEntity> getEntityValidator() {
        return entityValidator;
    }

    @Override
    public AppPermissionEntity save(AppPermissionEntity entity) throws EntityValidationException {
        validateEntity(entity);
        updateAuditTimestamps(entity);
        return appPermissionEntityRepository.save(entity);
    }

    @Override
    public List<AppPermissionEntity> getAllAppPermissions() {
        return appPermissionEntityRepository.findAll();
    }

    @Override
    public Page<AppPermissionEntity> getAppPermissions(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException {
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
            Specification<AppPermissionEntity> spec = SpecificationBuilder.build(expression);
            return appPermissionEntityRepository.findAll(spec, pageRequest);
        }

        return appPermissionEntityRepository.findAll(pageRequest);
    }

    @Override
    public Optional<AppPermissionEntity> getAppPermissionByName(String name) {
        return appPermissionEntityRepository.findByName(name);
    }

    @Override
    public AppPermissionEntity getAppPermission(String name) {
        return appPermissionEntityRepository.findByName(name).orElse(null);
    }

    @Override
    public AppPermissionEntity updateAppPermission(AppPermissionEntity entity) throws EntityValidationException {
        return save(entity);
    }

    @Override
    public void deleteAppPermission(String name) {
        appPermissionEntityRepository.findByName(name).ifPresent(entity ->
                appPermissionEntityRepository.deleteById(entity.getId()));
    }
}
