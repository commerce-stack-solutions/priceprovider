package de.ebusyness.priceproviderservice.service.currency;

import de.ebusyness.commons.exception.InvalidParameterException;
import de.ebusyness.commons.query.QueryExpression;
import de.ebusyness.commons.query.QueryParser;
import de.ebusyness.commons.query.SpecificationBuilder;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.commons.service.entity.validation.EntityValidator;
import de.ebusyness.commons.service.entity.validation.ValidationRule;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.priceproviderservice.dataaccess.currency.CurrencyEntityRepository;
import de.ebusyness.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import jakarta.transaction.Transactional;
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
 * Implementation of CurrencyService interface.
 * This class provides concrete implementation for currency management operations.
 */
@Service
public class CurrencyServiceImpl implements CurrencyService {

    private static final Logger logger = LoggerFactory.getLogger(CurrencyServiceImpl.class);

    private final CurrencyEntityRepository currencyEntityRepository;
    private final EntityValidator<CurrencyEntity> entityValidator;
    private final QueryParser queryParser;

    @Autowired
    public CurrencyServiceImpl(CurrencyEntityRepository currencyEntityRepository, List<ValidationRule<CurrencyEntity>> validationRules) {
        this.currencyEntityRepository = currencyEntityRepository;
        this.entityValidator = new EntityValidator<>(validationRules);
        this.queryParser = new QueryParser(CurrencyEntity.class);
    }

    @Override
    public Class<CurrencyEntity> getTargetClass() {
        return CurrencyEntity.class;
    }

    @Override
    public EntityValidator<CurrencyEntity> getEntityValidator() {
        return entityValidator;
    }

    public CurrencyEntity save(CurrencyEntity currencyEntity) throws EntityValidationException {
        validateEntity(currencyEntity);
        updateAuditTimestamps(currencyEntity);
        return currencyEntityRepository.save(currencyEntity);
    }

    @Transactional
    public void deleteCurrency(String currencyKey) {
        currencyEntityRepository.deleteById(currencyKey);
    }


    @Override
    public Page<CurrencyEntity> getCurrencies(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException {
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
            Specification<CurrencyEntity> spec = SpecificationBuilder.build(expression);
            return currencyEntityRepository.findAll(spec, pageRequest);
        }

        return currencyEntityRepository.findAll(pageRequest);
    }

    public CurrencyEntity getCurrency(String currencyKey) {
        return currencyEntityRepository.findById(currencyKey).orElse(null);
    }

}
