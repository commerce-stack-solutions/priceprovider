package io.commercestacksolutions.priceproviderservice.service.pricerow;

import io.commercestacksolutions.commons.exception.InvalidParameterException;
import io.commercestacksolutions.commons.query.QueryExpression;
import io.commercestacksolutions.commons.query.QueryParser;
import io.commercestacksolutions.commons.query.SpecificationBuilder;
import io.commercestacksolutions.commons.query.exception.QueryParseException;
import io.commercestacksolutions.commons.service.entity.validation.EntityValidator;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.service.entity.validation.exception.EntityValidationException;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.PriceRowEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.service.channel.ChannelService;
import io.commercestacksolutions.priceproviderservice.service.pricerow.smartmatching.PriceRowMatchingContext;
import io.commercestacksolutions.priceproviderservice.service.pricerow.smartmatching.SmartMatchingStrategy;
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
 * Implementation of PriceRowService interface.
 * This class provides concrete implementation for price row management operations.
 */
@Service
public class PriceRowServiceImpl implements PriceRowService {

    private static final Logger logger = LoggerFactory.getLogger(PriceRowServiceImpl.class);

    private final PriceRowEntityRepository priceRowEntityRepository;
    private final EntityValidator<PriceRowEntity> entityValidator;
    private final QueryParser queryParser;
    private final ChannelService channelService;
    private final SmartMatchingStrategy smartMatchingStrategy;

    @Autowired
    public PriceRowServiceImpl(
            PriceRowEntityRepository priceRowEntityRepository,
            List<ValidationRule<PriceRowEntity>> validationRules,
            ChannelService channelService,
            SmartMatchingStrategy smartMatchingStrategy) {
        this.priceRowEntityRepository = priceRowEntityRepository;
        this.entityValidator = new EntityValidator<>(validationRules);
        this.queryParser = new QueryParser(PriceRowEntity.class);
        this.channelService = channelService;
        this.smartMatchingStrategy = smartMatchingStrategy;
    }

    @Override
    public Class<PriceRowEntity> getTargetClass() {
        return PriceRowEntity.class;
    }

    @Override
    public EntityValidator<PriceRowEntity> getEntityValidator() {
        return entityValidator;
    }

    public PriceRowEntity save(PriceRowEntity priceRowEntity) throws EntityValidationException {
        validateEntity(priceRowEntity);
        updateAuditTimestamps(priceRowEntity);
        return priceRowEntityRepository.save(priceRowEntity);
    }

    public List<PriceRowEntity> findAll() {
        return priceRowEntityRepository.findAll();
    }

    public Page<PriceRowEntity> findAll(int page, int pageSize) {
        return priceRowEntityRepository.findAll(PageRequest.of(page, pageSize));
    }

    @Override
    public Page<PriceRowEntity> findAll(int page, int pageSize, List<String> sortBy, String sortDirection, String query) throws QueryParseException, InvalidParameterException {
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
            Specification<PriceRowEntity> spec = SpecificationBuilder.build(expression);
            return priceRowEntityRepository.findAll(spec, pageRequest);
        }

        return priceRowEntityRepository.findAll(pageRequest);
    }

    public Optional<PriceRowEntity> findById(Long id) {
        return priceRowEntityRepository.findById(id);
    }

    public void deleteById(Long id) {
        priceRowEntityRepository.deleteById(id);
    }

    @Override
    public Optional<PriceRowEntity> findByMatchingFields(PriceRowMatchingContext context) {
        return smartMatchingStrategy.findMatch(context);
    }
}