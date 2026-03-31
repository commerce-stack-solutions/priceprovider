package io.commercestacksolutions.priceproviderservice.service.unit;

import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.query.QueryExpression;
import io.commercestacksolutions.commons.query.QueryParser;
import io.commercestacksolutions.commons.query.SpecificationBuilder;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.EntityValidator;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.commons.web.rest.Message;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.UnitEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
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

/**
 * Implementation of UnitService interface.
 * This class provides concrete implementation for unit management operations.
 */
@Service
public class UnitServiceImpl implements UnitService {

    private static final Logger logger = LoggerFactory.getLogger(UnitServiceImpl.class);

    @Autowired
    private UnitEntityRepository unitEntityRepository;

    private final EntityValidator<UnitEntity> entityValidator;
    private final QueryParser queryParser;

    @Autowired
    public UnitServiceImpl(List<ValidationRule<UnitEntity>> validationRules) {
        this.entityValidator = new EntityValidator<>(validationRules);
        this.queryParser = new QueryParser(UnitEntity.class);
    }

    // Create operation
    @Override
    public Class<UnitEntity> getTargetClass() {
        return UnitEntity.class;
    }

    @Override
    public EntityValidator<UnitEntity> getEntityValidator() {
        return entityValidator;
    }

    @Override
    public void validateEntity(UnitEntity entity) throws EntityValidationException {
        List<Message> validationErrors = entityValidator.validate(entity);
        if (!validationErrors.isEmpty()) {
            // Throw UnitValidationException for backward compatibility
            // Use the first error message key as the exception message
            Message firstError = validationErrors.get(0);
            throw new EntityValidationException(firstError.getMessageKey(), validationErrors);
        }
    }

    public UnitEntity save(UnitEntity unitEntity) throws EntityValidationException {
        validateEntity(unitEntity);
        updateAuditTimestamps(unitEntity);
        return unitEntityRepository.save(unitEntity);
    }

    // Delete operation
    public void deleteUnit(String symbol) {
        unitEntityRepository.deleteById(symbol);
    }

    public UnitEntity findBySymbol(String symbol) {
        return unitEntityRepository.findBySymbol(symbol);
    }

    // Business logic methods from UnitService
    @Override
    public Page<UnitEntity> getUnits(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException {
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

        // Parse and apply query filter if provided
        if (query != null && !query.trim().isEmpty()) {
            QueryExpression expression = queryParser.parse(query);
            Specification<UnitEntity> spec = SpecificationBuilder.build(expression);
            return unitEntityRepository.findAll(spec, pageRequest);

        }

        return unitEntityRepository.findAll(pageRequest);
    }

    public UnitEntity getUnit(String symbol) {
        return unitEntityRepository.findById(symbol).orElse(null);
    }
}
