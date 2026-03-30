package de.ebusyness.priceproviderservice.service.taxclass;

import de.ebusyness.commons.query.*;
import de.ebusyness.commons.exception.InvalidParameterException;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.priceproviderservice.commons.messagekeys.MessageKeys;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.priceproviderservice.dataaccess.taxclass.TaxClassEntityRepository;
import de.ebusyness.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import de.ebusyness.commons.service.entity.validation.EntityValidator;
import de.ebusyness.commons.service.entity.validation.ValidationRule;
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
 * Implementation of TaxClassService interface.
 * This class provides concrete implementation for tax class management operations.
 */
@Service
public class TaxClassServiceImpl implements TaxClassService {

    private static final Logger logger = LoggerFactory.getLogger(TaxClassServiceImpl.class);

    private final TaxClassEntityRepository taxClassEntityRepository;
    private final EntityValidator<TaxClassEntity> entityValidator;
    private final QueryParser queryParser;

    @Autowired
    public TaxClassServiceImpl(TaxClassEntityRepository taxClassEntityRepository, List<ValidationRule<TaxClassEntity>> validationRules) {
        this.taxClassEntityRepository = taxClassEntityRepository;
        this.entityValidator = new EntityValidator<>(validationRules);
        // Create a QueryParser configured with the entity's field types so parser can validate types
        this.queryParser = new QueryParser(QueryReflectionUtil.buildFieldTypeMap(TaxClassEntity.class));
    }

    @Override
    public Class<TaxClassEntity> getTargetClass() {
        return TaxClassEntity.class;
    }

    @Override
    public EntityValidator<TaxClassEntity> getEntityValidator() {
        return entityValidator;
    }

    public TaxClassEntity save(TaxClassEntity taxClassEntity) throws EntityValidationException {
        validateEntity(taxClassEntity);
        updateAuditTimestamps(taxClassEntity);
        return taxClassEntityRepository.save(taxClassEntity);
    }

    public void deleteTaxClass(String taxClassId) {
        taxClassEntityRepository.deleteById(taxClassId);
    }


    @Override
    public Page<TaxClassEntity> getTaxClasses(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException {
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
            Specification<TaxClassEntity> spec = SpecificationBuilder.build(expression);
            return taxClassEntityRepository.findAll(spec, pageRequest);
        }

        return taxClassEntityRepository.findAll(pageRequest);
    }

    public TaxClassEntity getTaxClass(String taxClassId) {
        return taxClassEntityRepository.findById(taxClassId).orElse(null);
    }

}
