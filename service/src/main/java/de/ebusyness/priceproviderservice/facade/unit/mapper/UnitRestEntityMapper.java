package de.ebusyness.priceproviderservice.facade.unit.mapper;

import de.ebusyness.commons.mapper.AbstractMapper;
import de.ebusyness.commons.mapper.RestResponseMappingContext;
import de.ebusyness.commons.web.rest.InfoAuditableRestEntity;
import de.ebusyness.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import de.ebusyness.priceproviderservice.facade.unit.restentity.UnitRestEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class UnitRestEntityMapper extends AbstractMapper<UnitEntity, UnitRestEntity, RestResponseMappingContext> {

    @Override
    public UnitRestEntity createTarget() {
        return new UnitRestEntity();
    }

    @Override
    public void convert(UnitEntity source, UnitRestEntity target, RestResponseMappingContext context) {
        target.setSymbol(source.getSymbol());
        // Lazy loading safe: null-check und defensive copy
        Map<String, String> nameMap = source.getName();
        if (nameMap == null) {
            target.setName(new HashMap<>());
        } else {
            target.setName(new HashMap<>(nameMap));
        }
        target.setMeasure(source.getMeasure());
        // Lazy loading safe: Nur Symbol der Basiseinheit übertragen
        if (source.getBaseUnit() != null) {
            target.setBaseUnitRef(source.getBaseUnit().getSymbol());
        } else {
            target.setBaseUnitRef(null);
        }
        target.setFactor(source.getFactor());

        if (context.shouldExpand("$info")) {
            addInfoSection(source, target, context);
        }
    }

    private void addInfoSection(UnitEntity source, UnitRestEntity target, RestResponseMappingContext context) {
        // Add audit timestamps to $info
        InfoAuditableRestEntity info = new InfoAuditableRestEntity();
        if (context.expandWithAnyOf(new String[]{"$info", "$info.createdAt"})) {
            info.setCreatedAt(source.getCreatedAt());
        }
        if (context.expandWithAnyOf(new String[]{"$info", "$info.lastModifiedAt"})) {
            info.setLastModifiedAt(source.getLastModifiedAt());
        }
        target.setInfo(info);
    }
}