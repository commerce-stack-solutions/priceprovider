package de.ebusyness.priceproviderservice.service.pricerow;

import de.ebusyness.commons.exception.InvalidParameterException;
import de.ebusyness.commons.query.QueryExpression;
import de.ebusyness.commons.query.QueryParser;
import de.ebusyness.commons.query.SpecificationBuilder;
import de.ebusyness.commons.query.exception.QueryParseException;
import de.ebusyness.commons.service.entity.validation.EntityValidator;
import de.ebusyness.commons.service.entity.validation.ValidationRule;
import de.ebusyness.commons.service.entity.validation.exception.EntityValidationException;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.PriceRowEntityRepository;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.enums.PriceType;
import de.ebusyness.priceproviderservice.service.channel.ChannelService;
import de.ebusyness.priceproviderservice.service.pricerow.smartmatching.SmartMatchingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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
    public Optional<PriceRowEntity> findByMatchingFields(
            String pricedResourceId,
            BigDecimal minQuantity,
            String unitRef,
            String currencyRef,
            String taxClassRef,
            boolean taxIncluded,
            PriceType priceType,
            OffsetDateTime validFrom,
            OffsetDateTime validTo,
            Set<String> groupRefs) {

        return smartMatchingStrategy.findMatch(
                pricedResourceId,
                minQuantity,
                unitRef,
                currencyRef,
                taxClassRef,
                taxIncluded,
                priceType,
                validFrom,
                validTo,
                groupRefs
        );
    }
}