package de.ebusyness.priceproviderservice.facade.organization.mapper;

import de.ebusyness.commons.mapper.AbstractMapper;
import de.ebusyness.commons.mapper.RestResponseMappingContext;
import de.ebusyness.commons.web.rest.InfoAuditableRestEntity;
import de.ebusyness.priceproviderservice.dataaccess.group.entity.GroupEntity;
import de.ebusyness.priceproviderservice.dataaccess.organization.entity.OrganizationEntity;
import de.ebusyness.priceproviderservice.facade.organization.restentity.OrganizationRestEntity;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

@Component
public class OrganizationRestEntityMapper extends AbstractMapper<OrganizationEntity, OrganizationRestEntity, RestResponseMappingContext> {

    @Override
    public OrganizationRestEntity createTarget() {
        return new OrganizationRestEntity();
    }

    @Override
    public void convert(OrganizationEntity source, OrganizationRestEntity target, RestResponseMappingContext context) {
        target.setId(source.getId());
        target.setName(source.getName());
        target.setOrganizationType(source.getOrganizationType());

        // Convert parent entities to IDs
        if (source.getParentRefs() != null) {
            Set<String> parentRefs = source.getParentRefs().stream()
                    .filter(parent -> parent != null && parent.getId() != null)
                    .map(GroupEntity::getId)
                    .collect(Collectors.toSet());
            target.setParentRefs(parentRefs);
        }

        // Convert sub entities to IDs
        if (source.getSubRefs() != null) {
            Set<String> subRefs = source.getSubRefs().stream()
                    .filter(sub -> sub != null && sub.getId() != null)
                    .map(GroupEntity::getId)
                    .collect(Collectors.toSet());
            target.setSubRefs(subRefs);
        }

        if (context.shouldExpand("$info")) {
            addInfoSection(source, target, context);
        }
    }

    private void addInfoSection(OrganizationEntity source, OrganizationRestEntity target, RestResponseMappingContext context) {
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
