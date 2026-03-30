package de.ebusyness.priceproviderservice.service.country;

import de.ebusyness.commons.exception.InvalidParameterException;
import de.ebusyness.commons.query.*;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.commons.service.entity.validation.EntityValidator;
import de.ebusyness.commons.service.entity.validation.ValidationRule;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.priceproviderservice.dataaccess.country.CountryEntityRepository;
import de.ebusyness.priceproviderservice.dataaccess.country.entity.CountryEntity;
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
 * Implementation of CountryService interface.
 * This class provides concrete implementation for country management operations.
 */
@Service
public class CountryServiceImpl implements CountryService {

    private static final Logger logger = LoggerFactory.getLogger(CountryServiceImpl.class);

    private final CountryEntityRepository countryEntityRepository;
    private final EntityValidator<CountryEntity> entityValidator;
    private final QueryParser queryParser;

    @Autowired
    public CountryServiceImpl(CountryEntityRepository countryEntityRepository, List<ValidationRule<CountryEntity>> validationRules) {
        this.countryEntityRepository = countryEntityRepository;
        this.entityValidator = new EntityValidator<>(validationRules);
        // Create a QueryParser configured with the entity's field types so parser can validate types
        this.queryParser = new QueryParser(QueryReflectionUtil.buildFieldTypeMap(CountryEntity.class));
    }

    @Override
    public Class<CountryEntity> getTargetClass() {
        return CountryEntity.class;
    }

    @Override
    public EntityValidator<CountryEntity> getEntityValidator() {
        return entityValidator;
    }

    @Override
    public CountryEntity save(CountryEntity countryEntity) throws EntityValidationException {
        validateEntity(countryEntity);
        updateAuditTimestamps(countryEntity);
        return countryEntityRepository.save(countryEntity);
    }

    @Override
    public void deleteCountry(String isoKey) {
        countryEntityRepository.deleteById(isoKey);
    }

    @Override
    public Page<CountryEntity> getCountries(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException {
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
            Specification<CountryEntity> spec = SpecificationBuilder.build(expression);
            return countryEntityRepository.findAll(spec, pageRequest);
        }

        return countryEntityRepository.findAll(pageRequest);
    }

    @Override
    public CountryEntity getCountry(String isoKey) {
        return countryEntityRepository.findByIdWithCurrencies(isoKey).orElse(null);
    }
}
