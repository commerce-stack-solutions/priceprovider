package io.commercestacksolutions.priceproviderservice.facade.taxclass.mapper;

import io.commercestacksolutions.commons.mapper.AbstractMapper;
import io.commercestacksolutions.commons.mapper.RestResponseMappingContext;
import io.commercestacksolutions.commons.web.rest.InfoAuditableRestEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import io.commercestacksolutions.priceproviderservice.facade.taxclass.restentity.TaxClassRestEntity;
import org.springframework.stereotype.Component;

@Component
public class TaxClassRestEntityMapper extends AbstractMapper<TaxClassEntity, TaxClassRestEntity, RestResponseMappingContext> {

    @Override
    public TaxClassRestEntity createTarget() {
        return new TaxClassRestEntity();
    }

    @Override
    public void convert(TaxClassEntity source, TaxClassRestEntity target, RestResponseMappingContext context) {
        target.setTaxClassId(source.getTaxClassId());
        target.setTaxRate(source.getTaxRate());
        target.setCountryRef(source.getCountryRef());

        if(context.shouldExpand("$info")) {
            addInfoSection(source, target, context);
        }
    }

    private void addInfoSection(TaxClassEntity source, TaxClassRestEntity target, RestResponseMappingContext context) {
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
