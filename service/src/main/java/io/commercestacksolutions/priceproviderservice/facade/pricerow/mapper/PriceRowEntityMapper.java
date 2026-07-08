package io.commercestacksolutions.priceproviderservice.facade.pricerow.mapper;

import io.commercestacksolutions.commons.mapper.AbstractMapper;
import io.commercestacksolutions.commons.mapper.RestRequestMappingContext;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.priceproviderservice.commons.messagekeys.MessageKeys;
import io.commercestacksolutions.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.pricetype.PriceType;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import io.commercestacksolutions.priceproviderservice.facade.pricerow.restentity.PriceRowRestEntity;
import io.commercestacksolutions.priceproviderservice.service.currency.CurrencyService;
import io.commercestacksolutions.priceproviderservice.service.group.GroupService;
import io.commercestacksolutions.priceproviderservice.service.taxclass.TaxClassService;
import io.commercestacksolutions.priceproviderservice.service.unit.UnitService;
import io.commercestacksolutions.priceproviderservice.service.channel.ChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import java.util.HashSet;
import java.util.Set;

@Component
public class PriceRowEntityMapper extends AbstractMapper<PriceRowRestEntity, PriceRowEntity, RestRequestMappingContext<String>> {
    
    private final UnitService unitEntityService;
    private final CurrencyService currencyEntityService;
    private final TaxClassService taxClassEntityService;
    private final GroupService groupEntityService;
    private final ChannelService channelEntityService;

    @Autowired
    public PriceRowEntityMapper(UnitService unitEntityService,
                                CurrencyService currencyEntityService,
                                TaxClassService taxClassEntityService,
                                GroupService groupEntityService,
                                ChannelService channelEntityService) {
        this.unitEntityService = unitEntityService;
        this.currencyEntityService = currencyEntityService;
        this.taxClassEntityService = taxClassEntityService;
        this.groupEntityService = groupEntityService;
        this.channelEntityService = channelEntityService;
    }

    @Override
    public PriceRowEntity createTarget() {
        return new PriceRowEntity();
    }

    @Override
    public void convert(PriceRowRestEntity source, PriceRowEntity target, RestRequestMappingContext<String> context) throws DataMappingException {
        target.setId(context.getId());
        target.setPricedResourceId(source.getPricedResourceId());
        target.setPriceValue(source.getPriceValue());
        target.setMinQuantity(source.getMinQuantity());
        
        // Fetch the actual unit entity from database instead of creating a transient one
        if (source.getUnitRef() != null && !source.getUnitRef().isEmpty()) {
            UnitEntity unit = unitEntityService.getUnit(source.getUnitRef());
            if (unit == null) {
                Map<String, String> params = new HashMap<>();
                params.put("entityType", "Unit");
                params.put("idField", "symbol");
                params.put("id", source.getUnitRef());
                throw new DataMappingException(MessageKeys.ERROR_MAPPING_ENTITY_NOT_FOUND, params);
            }
            target.setUnit(unit);
        } else {
            target.setUnit(null);
        }
        
        // Fetch the actual currency entity from database
        if (source.getCurrencyRef() != null && !source.getCurrencyRef().isEmpty()) {
            CurrencyEntity currency = currencyEntityService.getCurrency(source.getCurrencyRef());
            if (currency == null) {
                Map<String, String> params = new HashMap<>();
                params.put("entityType", "Currency");
                params.put("idField", "currencyKey");
                params.put("id", source.getCurrencyRef());
                throw new DataMappingException(MessageKeys.ERROR_MAPPING_ENTITY_NOT_FOUND, params);
            }
            target.setCurrency(currency);
        } else {
            target.setCurrency(null);
        }
        
        // Fetch the actual tax class entity from database (mandatory)
        if (source.getTaxClassRef() != null && !source.getTaxClassRef().isEmpty()) {
            TaxClassEntity taxClass = taxClassEntityService.getTaxClass(source.getTaxClassRef());
            if (taxClass == null) {
                Map<String, String> params = new HashMap<>();
                params.put("entityType", "TaxClass");
                params.put("idField", "taxClassId");
                params.put("id", source.getTaxClassRef());
                throw new DataMappingException(MessageKeys.ERROR_MAPPING_ENTITY_NOT_FOUND, params);
            }
            target.setTaxClass(taxClass);
        } else {
            throw new DataMappingException(MessageKeys.ERROR_MAPPING_TAX_CLASS_MANDATORY);
        }

        target.setPriceType(source.getPriceType() != null ? new PriceType(source.getPriceType()) : null);
        target.setValidFrom(source.getValidFrom());
        target.setValidTo(source.getValidTo());

        // Fetch the actual group entities from database (optional, but must be valid if provided)
        if (source.getGroupRefs() != null && !source.getGroupRefs().isEmpty()) {
            Set<GroupEntity> groupRefs = new HashSet<>();
            for (String groupRef : source.getGroupRefs()) {
                GroupEntity group = groupEntityService.getGroupByPath(groupRef);
                if (group == null) {
                    throw new DataMappingException("Group with path '" + groupRef + "' not found");
                }
                groupRefs.add(group);
            }
            target.setGroups(groupRefs);
        } else {
            target.setGroups(new HashSet<>());
        }

        target.setTaxIncluded(source.isTaxIncluded());

        // Fetch the actual channel entities from database (optional, but must be valid if provided)
        if (source.getChannelRefs() != null && !source.getChannelRefs().isEmpty()) {
            Set<ChannelEntity> channelRefs = new HashSet<>();
            for (String channelRef : source.getChannelRefs()) {
                ChannelEntity channel = channelEntityService.getChannel(channelRef);
                if (channel == null) {
                    Map<String, String> params = new HashMap<>();
                    params.put("entityType", "Channel");
                    params.put("idField", "id");
                    params.put("id", channelRef);
                    throw new DataMappingException(MessageKeys.ERROR_MAPPING_ENTITY_NOT_FOUND, params);
                }
                channelRefs.add(channel);
            }
            target.setChannels(channelRefs);
        } else {
            target.setChannels(new HashSet<>());
        }
    }
}