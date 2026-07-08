package io.commercestacksolutions.priceproviderservice.facade.publicprice.mapper;

import io.commercestacksolutions.commons.mapper.AbstractMapper;
import io.commercestacksolutions.commons.mapper.RestResponseMappingContext;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.facade.currency.mapper.CurrencyRestEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.publicprice.info.InfoPublicPrice;
import io.commercestacksolutions.priceproviderservice.facade.publicprice.info.OriginalPriceInfo;
import io.commercestacksolutions.priceproviderservice.facade.publicprice.info.TaxationInfo;
import io.commercestacksolutions.priceproviderservice.facade.publicprice.restentity.IncludesPublicPrice;
import io.commercestacksolutions.priceproviderservice.facade.publicprice.restentity.PublicPriceRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.taxclass.mapper.TaxClassRestEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.unit.mapper.UnitRestEntityMapper;
import io.commercestacksolutions.priceproviderservice.service.publicprice.model.PriceMatchingCriteria;
import io.commercestacksolutions.priceproviderservice.service.publicprice.strategy.TaxCalculationStrategy;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

/**
 * Mapper for converting PriceRowEntity to PublicPriceRestEntity.
 * 
 * This mapper applies tax calculations based on the requested taxation mode:
 * - NET: Returns net price (tax excluded)
 * - GROSS: Returns gross price (tax included)
 * - AS_DECLARED: Returns price as stored in the database
 * 
 * The original values are preserved in the $info section when expanded.
 */
@Component
public class PublicPriceMapper extends AbstractMapper<PriceRowEntity, PublicPriceRestEntity, RestResponseMappingContext> {
    
    private final UnitRestEntityMapper unitRestEntityMapper;
    private final CurrencyRestEntityMapper currencyRestEntityMapper;
    private final TaxClassRestEntityMapper taxClassRestEntityMapper;
    private final TaxCalculationStrategy taxCalculationStrategy;
    
    public PublicPriceMapper(
            UnitRestEntityMapper unitRestEntityMapper,
            CurrencyRestEntityMapper currencyRestEntityMapper,
            TaxClassRestEntityMapper taxClassRestEntityMapper,
            TaxCalculationStrategy taxCalculationStrategy) {
        this.unitRestEntityMapper = unitRestEntityMapper;
        this.currencyRestEntityMapper = currencyRestEntityMapper;
        this.taxClassRestEntityMapper = taxClassRestEntityMapper;
        this.taxCalculationStrategy = taxCalculationStrategy;
    }
    
    @Override
    public PublicPriceRestEntity createTarget() {
        return new PublicPriceRestEntity();
    }
    
    @Override
    public void convert(PriceRowEntity source, PublicPriceRestEntity target, RestResponseMappingContext context) throws DataMappingException {
        // Get taxation mode from context (injected by facade)
        PriceMatchingCriteria.TaxationMode taxationMode = 
                (PriceMatchingCriteria.TaxationMode) context.getProperty("taxationMode");
        if (taxationMode == null) {
            taxationMode = PriceMatchingCriteria.TaxationMode.GROSS; // Default
        }
        
        target.setId(source.getId());
        target.setPricedResourceId(source.getPricedResourceId());
        target.setMinQuantity(source.getMinQuantity());
        
        // Calculate price value based on taxation mode
        BigDecimal calculatedPrice = calculatePrice(source, taxationMode);
        target.setPriceValue(calculatedPrice);
        
        // Set taxIncluded flag based on the calculated price
        target.setTaxIncluded(determineTaxIncluded(source, taxationMode));
        
        // Map entity references to string references
        target.setUnitRef(source.getUnit() != null ? source.getUnit().getSymbol() : null);
        target.setCurrencyRef(source.getCurrency() != null ? source.getCurrency().getCurrencyKey() : null);
        target.setTaxClassRef(source.getTaxClass() != null ? source.getTaxClass().getTaxClassId() : null);

        target.setPriceType(source.getPriceType() != null ? source.getPriceType().code() : null);
        target.setValidFrom(source.getValidFrom());
        target.setValidTo(source.getValidTo());
        
        // Map group references to string paths
        if (source.getGroups() != null) {
            Set<String> groupRefIds = new HashSet<>();
            for (GroupEntity group : source.getGroups()) {
                if (group != null && group.getPath() != null) {
                    groupRefIds.add(group.getPath());
                }
            }
            target.setGroupRefs(groupRefIds);
        }

        // Map channel references to string IDs
        if (source.getChannels() != null) {
            Set<String> channelRefIds = new HashSet<>();
            for (io.commercestacksolutions.priceproviderservice.dataaccess.channel.entity.ChannelEntity channel : source.getChannels()) {
                if (channel != null && channel.getId() != null) {
                    channelRefIds.add(channel.getId());
                }
            }
            target.setChannelRefs(channelRefIds);
        }

        // Add info section if requested via $expand
        if (context.shouldExpand("$info")) {
            addInfo(source, target, taxationMode, context);
        }
        
        // Add includes section if requested via $expand
        if (context.shouldExpand("$includes")) {
            addIncludes(source, target, context);
        }
    }
    
    /**
     * Calculates the price value based on the taxation mode.
     */
    private BigDecimal calculatePrice(PriceRowEntity source, PriceMatchingCriteria.TaxationMode taxationMode) {
        if (source.getPriceValue() == null || source.getTaxClass() == null) {
            return source.getPriceValue();
        }
        
        BigDecimal originalPrice = source.getPriceValue();
        BigDecimal taxRate = source.getTaxClass().getTaxRate();
        boolean originalTaxIncluded = source.isTaxIncluded();
        
        switch (taxationMode) {
            case NET:
                // Return net price (tax excluded)
                if (originalTaxIncluded) {
                    // Convert from gross to net
                    return taxCalculationStrategy.calculateNetFromGross(originalPrice, taxRate);
                } else {
                    // Already net
                    return originalPrice;
                }
                
            case GROSS:
                // Return gross price (tax included)
                if (originalTaxIncluded) {
                    // Already gross
                    return originalPrice;
                } else {
                    // Convert from net to gross
                    return taxCalculationStrategy.calculateGrossFromNet(originalPrice, taxRate);
                }
                
            case AS_DECLARED:
            default:
                // Return as stored
                return originalPrice;
        }
    }
    
    /**
     * Determines the taxIncluded flag for the calculated price.
     */
    private boolean determineTaxIncluded(PriceRowEntity source, PriceMatchingCriteria.TaxationMode taxationMode) {
        switch (taxationMode) {
            case NET:
                return false;
            case GROSS:
                return true;
            case AS_DECLARED:
            default:
                return source.isTaxIncluded();
        }
    }
    
    /**
     * Adds info section with taxation info and original price info.
     */
    private void addInfo(PriceRowEntity source, PublicPriceRestEntity target, 
                        PriceMatchingCriteria.TaxationMode taxationMode, RestResponseMappingContext context) {
        InfoPublicPrice info = new InfoPublicPrice();
        
        // Add taxation info
        if (source.getTaxClass() != null && source.getPriceValue() != null &&
                context.expandWithAnyOf(new String[]{"$info", "$info.taxation"})) {
            addTaxationInfo(source, target, info);
        }
        
        // Add original price info (always included in $info for public API)
        if (context.expandWithAnyOf(new String[]{"$info", "$info.originalPrice"})) {
            OriginalPriceInfo originalPrice = new OriginalPriceInfo(
                    source.getPriceValue(),
                    source.isTaxIncluded()
            );
            info.setOriginalPrice(originalPrice);
        }
        
        target.setInfo(info);
    }
    
    /**
     * Adds taxation information based on the calculated price.
     */
    private void addTaxationInfo(PriceRowEntity source, PublicPriceRestEntity target, InfoPublicPrice info) {
        BigDecimal taxRate = source.getTaxClass().getTaxRate();
        BigDecimal calculatedPrice = target.getPriceValue();
        BigDecimal taxValue;
        
        if (target.isTaxIncluded()) {
            // Calculated price includes tax - calculate tax portion
            taxValue = taxCalculationStrategy.calculateTaxFromGross(calculatedPrice, taxRate);
        } else {
            // Calculated price excludes tax - calculate tax to add
            taxValue = taxCalculationStrategy.calculateTaxFromNet(calculatedPrice, taxRate);
        }
        
        String taxIncludedInfo = target.isTaxIncluded() ? "included (gross)" : "to be added (net)";
        
        TaxationInfo taxation = new TaxationInfo(taxValue, taxRate, taxIncludedInfo);
        info.setTaxation(taxation);
    }
    
    /**
     * Adds includes section with related entities.
     */
    private void addIncludes(PriceRowEntity source, PublicPriceRestEntity target, RestResponseMappingContext context) throws DataMappingException {
        boolean hasAnyIncludes = false;
        IncludesPublicPrice includesObject = new IncludesPublicPrice();
        
        // Check if unit should be included
        if (context.expandWithAnyOf(new String[]{"$includes", "$includes.unit"})
                && source.getUnit() != null) {
            includesObject.setUnit(unitRestEntityMapper.convert(source.getUnit(), context));
            hasAnyIncludes = true;
        }
        
        // Check if currency should be included
        if (context.expandWithAnyOf(new String[]{"$includes", "$includes.currency"})
                && source.getCurrency() != null) {
            includesObject.setCurrency(currencyRestEntityMapper.convert(source.getCurrency(), context));
            hasAnyIncludes = true;
        }
        
        // Check if taxClass should be included
        if (context.expandWithAnyOf(new String[]{"$includes", "$includes.taxClass"})
                && source.getTaxClass() != null) {
            includesObject.setTaxClass(taxClassRestEntityMapper.convert(source.getTaxClass(), context));
            hasAnyIncludes = true;
        }
        
        // Only set includes if at least one field was added
        if (hasAnyIncludes) {
            target.setIncludes(includesObject);
        }
    }
}
