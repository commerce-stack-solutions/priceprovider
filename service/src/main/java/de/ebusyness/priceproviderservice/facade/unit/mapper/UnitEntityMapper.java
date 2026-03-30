package de.ebusyness.priceproviderservice.facade.unit.mapper;

import de.ebusyness.commons.mapper.AbstractMapper;
import de.ebusyness.commons.mapper.RestRequestMappingContext;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.priceproviderservice.commons.messagekeys.MessageKeys;
import de.ebusyness.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import de.ebusyness.priceproviderservice.facade.unit.restentity.UnitRestEntity;
import de.ebusyness.priceproviderservice.service.unit.UnitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class UnitEntityMapper extends AbstractMapper<UnitRestEntity, UnitEntity, RestRequestMappingContext<String>> {
    
    private final UnitService unitEntityService;

    @Autowired
    public UnitEntityMapper(UnitService unitEntityService) {
        this.unitEntityService = unitEntityService;
    }

    @Override
    public UnitEntity createTarget() {
        return new UnitEntity();
    }

    @Override
    public void convert(UnitRestEntity source, UnitEntity target, RestRequestMappingContext<String> context) throws DataMappingException {
        target.setSymbol(context.getId());
        target.setName(source.getName() != null ? new HashMap<>(source.getName()) : new HashMap<>());
        target.setMeasure(source.getMeasure());
        target.setFactor(source.getFactor());
        
        // Handle baseUnitRef
        if (source.getBaseUnitRef() != null && !source.getBaseUnitRef().isEmpty()) {
            UnitEntity baseUnitRef = unitEntityService.getUnit(source.getBaseUnitRef());
            if (baseUnitRef == null) {
                Map<String, String> params = new HashMap<>();
                params.put("entityType", "Unit");
                params.put("idField", "symbol");
                params.put("id", source.getBaseUnitRef());
                throw new DataMappingException(MessageKeys.ERROR_MAPPING_ENTITY_NOT_FOUND, params);
            }
            target.setBaseUnit(baseUnitRef);
        } else {
            target.setBaseUnit(null);
        }
    }
}
