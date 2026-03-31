package io.commercestacksolutions.priceproviderservice.service.language;

import io.commercestacksolutions.commons.query.*;
import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.dataaccess.language.LanguageEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import io.commercestacksolutions.commons.service.entity.validation.EntityValidator;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
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

/**
 * Implementation of LanguageService interface.
 * This class provides concrete implementation for language management operations.
 */
@Service
public class LanguageServiceImpl implements LanguageService {

    private static final Logger logger = LoggerFactory.getLogger(LanguageServiceImpl.class);

    private final LanguageEntityRepository languageEntityRepository;
    private final EntityValidator<LanguageEntity> entityValidator;
    private final QueryParser queryParser;

    @Autowired
    public LanguageServiceImpl(LanguageEntityRepository languageEntityRepository, List<ValidationRule<LanguageEntity>> validationRules) {
        this.languageEntityRepository = languageEntityRepository;
        this.entityValidator = new EntityValidator<>(validationRules);
        this.queryParser = new QueryParser(LanguageEntity.class);
    }

    @Override
    public Class<LanguageEntity> getTargetClass() {
        return LanguageEntity.class;
    }

    @Override
    public EntityValidator<LanguageEntity> getEntityValidator() {
        return entityValidator;
    }

    public LanguageEntity save(LanguageEntity languageEntity) throws EntityValidationException {
        validateEntity(languageEntity);
        updateAuditTimestamps(languageEntity);
        return languageEntityRepository.save(languageEntity);
    }

    public List<LanguageEntity> getAllLanguages() {
        return languageEntityRepository.findAll();
    }

    public List<LanguageEntity> getActiveLanguages() {
        return languageEntityRepository.findByActive(true);
    }

    public List<LanguageEntity> getMandatoryLanguages() {
        return languageEntityRepository.findByMandatory(true);
    }

    public Optional<LanguageEntity> getLanguageByIsoKey(String isoKey) {
        return languageEntityRepository.findById(isoKey);
    }

    public LanguageEntity updateLanguage(LanguageEntity updatedLanguage) throws EntityValidationException {
        return save(updatedLanguage);
    }

    public void deleteLanguage(String isoKey) {
        languageEntityRepository.deleteById(isoKey);
    }

    public LanguageEntity findByIsoKey(String isoKey) {
        return languageEntityRepository.findByIsoKey(isoKey);
    }

    // Business logic methods from LanguageService
    @Override
    public Page<LanguageEntity> getLanguages(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException {
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
            Specification<LanguageEntity> spec = SpecificationBuilder.build(expression);
            return languageEntityRepository.findAll(spec, pageRequest);
        }

        return languageEntityRepository.findAll(pageRequest);
    }

    public LanguageEntity getLanguage(String isoKey) {
        return languageEntityRepository.findById(isoKey).orElse(null);
    }
}
