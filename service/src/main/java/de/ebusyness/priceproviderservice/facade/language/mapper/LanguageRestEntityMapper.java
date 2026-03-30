package de.ebusyness.priceproviderservice.facade.language.mapper;

import de.ebusyness.commons.mapper.AbstractMapper;
import de.ebusyness.commons.mapper.RestResponseMappingContext;
import de.ebusyness.commons.web.rest.InfoAuditableRestEntity;
import de.ebusyness.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import de.ebusyness.priceproviderservice.facade.language.restentity.LanguageRestEntity;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class LanguageRestEntityMapper extends AbstractMapper<LanguageEntity, LanguageRestEntity, RestResponseMappingContext> {

    @Override
    public LanguageRestEntity createTarget() {
        return new LanguageRestEntity();
    }

    @Override
    public void convert(LanguageEntity source, LanguageRestEntity target, RestResponseMappingContext context) {
        target.setIsoKey(source.getIsoKey());
        target.setActive(source.getActive());
        target.setMandatory(source.getMandatory());

        Map<String, String> names = source.getName();
        if (names != null) {
            target.setName(new HashMap<>(names));
        } else {
            target.setName(null);
        }

        if(context.shouldExpand("$info")) {
            addInfoSection(source, target, context);
        }
    }

    private void addInfoSection(LanguageEntity source, LanguageRestEntity target, RestResponseMappingContext context) {
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
