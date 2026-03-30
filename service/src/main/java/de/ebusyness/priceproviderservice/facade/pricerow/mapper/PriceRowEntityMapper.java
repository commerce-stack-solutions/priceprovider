package de.ebusyness.priceproviderservice.facade.pricerow.mapper;

import de.ebusyness.commons.mapper.AbstractMapper;
import de.ebusyness.commons.mapper.RestRequestMappingContext;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.priceproviderservice.commons.messagekeys.MessageKeys;
import de.ebusyness.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
import de.ebusyness.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import de.ebusyness.priceproviderservice.dataaccess.group.entity.GroupEntity;
import de.ebusyness.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import de.ebusyness.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import de.ebusyness.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import de.ebusyness.priceproviderservice.facade.pricerow.restentity.PriceRowRestEntity;
import de.ebusyness.priceproviderservice.service.currency.CurrencyService;
import de.ebusyness.priceproviderservice.service.group.GroupService;
import de.ebusyness.priceproviderservice.service.taxclass.TaxClassService;
import de.ebusyness.priceproviderservice.service.unit.UnitService;
import de.ebusyness.priceproviderservice.service.channel.ChannelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import java.util.HashSet;
import java.util.Set;

@Component
public class PriceRowEntityMapper extends AbstractMapper<PriceRowRestEntity, PriceRowEntity, RestRequestMappingContext<Long>> {
    
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
    public void convert(PriceRowRestEntity source, PriceRowEntity target, RestRequestMappingContext<Long> context) throws DataMappingException {
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
        
        target.setPriceType(source.getPriceType());
        target.setValidFrom(source.getValidFrom());
        target.setValidTo(source.getValidTo());

        // Fetch the actual group entities from database (optional, but must be valid if provided)
        if (source.getGroupRefs() != null && !source.getGroupRefs().isEmpty()) {
            Set<GroupEntity> groupRefs = new HashSet<>();
            for (String groupRef : source.getGroupRefs()) {
                GroupEntity group = groupEntityService.getGroup(groupRef);
                if (group == null) {
                    throw new DataMappingException("Group with id '" + groupRef + "' not found");
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