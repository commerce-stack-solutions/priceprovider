package io.commercestacksolutions.priceproviderservice.facade.pricerow.mapper;

import io.commercestacksolutions.commons.mapper.AbstractMapper;
import io.commercestacksolutions.commons.mapper.RestResponseMappingContext;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.facade.currency.mapper.CurrencyRestEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.pricerow.info.InfoPriceRow;
import io.commercestacksolutions.priceproviderservice.facade.pricerow.info.TaxationInfo;
import io.commercestacksolutions.priceproviderservice.facade.pricerow.restentity.IncludesPriceRow;
import io.commercestacksolutions.priceproviderservice.facade.pricerow.restentity.PriceRowRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.taxclass.mapper.TaxClassRestEntityMapper;
import io.commercestacksolutions.priceproviderservice.facade.unit.mapper.UnitRestEntityMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Set;

@Component
public class PriceRowRestEntityMapper extends AbstractMapper<PriceRowEntity, PriceRowRestEntity, RestResponseMappingContext> {

    private final UnitRestEntityMapper unitRestEntityMapper;
    private final CurrencyRestEntityMapper currencyRestEntityMapper;
    private final TaxClassRestEntityMapper taxClassRestEntityMapper;

    public PriceRowRestEntityMapper(UnitRestEntityMapper unitRestEntityMapper,
                                    CurrencyRestEntityMapper currencyRestEntityMapper,
                                    TaxClassRestEntityMapper taxClassRestEntityMapper) {
        this.unitRestEntityMapper = unitRestEntityMapper;
        this.currencyRestEntityMapper = currencyRestEntityMapper;
        this.taxClassRestEntityMapper = taxClassRestEntityMapper;
    }

    @Override
    public PriceRowRestEntity createTarget() {
        return new PriceRowRestEntity();
    }

    @Override
    public void convert(PriceRowEntity source, PriceRowRestEntity target, RestResponseMappingContext context) throws DataMappingException {
        target.setId(source.getId());
        target.setPricedResourceId(source.getPricedResourceId());
        target.setPriceValue(source.getPriceValue());
        target.setMinQuantity(source.getMinQuantity());
        
        // Map entity references to string references
        target.setUnitRef(source.getUnit() != null ? source.getUnit().getSymbol() : null);
        target.setCurrencyRef(source.getCurrency() != null ? source.getCurrency().getCurrencyKey() : null);
        target.setTaxClassRef(source.getTaxClass() != null ? source.getTaxClass().getTaxClassId() : null);
        
        target.setPriceType(source.getPriceType());
        target.setValidFrom(source.getValidFrom());
        target.setValidTo(source.getValidTo());
        
        // Map group references to string IDs
        if (source.getGroups() != null) {
            Set<String> groupRefIds = new HashSet<>();
            for (GroupEntity group : source.getGroups()) {
                if (group != null && group.getId() != null) {
                    groupRefIds.add(group.getId());
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
        
        target.setTaxIncluded(source.isTaxIncluded());

        // Add taxation info only if requested via $expand
        if (context.shouldExpand("$info")) {
            addInfo(source, target, context);
        }

        // Add includes only if requested via $expand
        if (context.shouldExpand("$includes")) {
            addIncludes(source, target, context);
        }
    }

    private void addInfo(PriceRowEntity source, PriceRowRestEntity target, RestResponseMappingContext context) {
        InfoPriceRow info = new InfoPriceRow();
        if (source.getTaxClass() != null && source.getPriceValue() != null &&
                context.expandWithAnyOf(new String[]{"$info", "$info.taxation"})) {
            addTaxation(source, info);
        }
        target.setInfo(info);
    }

    private static void addTaxation(PriceRowEntity source, InfoPriceRow info) {
        BigDecimal taxRate = source.getTaxClass().getTaxRate();
        BigDecimal priceValue = source.getPriceValue();
        BigDecimal taxValue;

        if (source.isTaxIncluded()) {
            // Price includes tax - calculate tax portion
            taxValue = priceValue.subtract(priceValue.divide(BigDecimal.ONE.add(taxRate), 2, RoundingMode.HALF_UP));
        } else {
            // Tax to be added - calculate tax to add
            taxValue = priceValue.multiply(taxRate).setScale(2, RoundingMode.HALF_UP);
        }

        String taxIncludedInfo = source.isTaxIncluded() ? "included (gross)" : "to be added (net)";

        TaxationInfo taxation = new TaxationInfo(taxValue, taxRate, taxIncludedInfo);
        info.setTaxation(taxation);

        // Add audit timestamps
        info.setCreatedAt(source.getCreatedAt());
        info.setLastModifiedAt(source.getLastModifiedAt());
    }

    private void addIncludes(PriceRowEntity source, PriceRowRestEntity target, RestResponseMappingContext context) throws DataMappingException {
        boolean hasAnyIncludes = false;
        IncludesPriceRow includesObject = new IncludesPriceRow();

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