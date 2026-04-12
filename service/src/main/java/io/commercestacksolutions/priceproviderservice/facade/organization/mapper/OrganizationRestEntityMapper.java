package io.commercestacksolutions.priceproviderservice.facade.organization.mapper;

import io.commercestacksolutions.commons.mapper.AbstractMapper;
import io.commercestacksolutions.commons.mapper.RestResponseMappingContext;
import io.commercestacksolutions.commons.web.rest.InfoAuditableRestEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.organization.entity.OrganizationEntity;
import io.commercestacksolutions.priceproviderservice.facade.organization.restentity.OrganizationRestEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
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
        target.setPath(source.getPath());
        target.setName(source.getName());
        target.setOrganizationType(source.getOrganizationType());

        // Convert parent entities to paths and build path→id map for navigation
        if (source.getParentRefs() != null) {
            Set<String> parentRefs = source.getParentRefs().stream()
                    .filter(parent -> parent != null && parent.getPath() != null)
                    .map(GroupEntity::getPath)
                    .collect(Collectors.toSet());
            target.setParentRefs(parentRefs);

            Map<String, String> parentRefIds = source.getParentRefs().stream()
                    .filter(parent -> parent != null && parent.getPath() != null && parent.getId() != null)
                    .collect(Collectors.toMap(GroupEntity::getPath, GroupEntity::getId));
            target.setParentRefIds(parentRefIds);
        }

        // Convert sub entities to paths and build path→id map for navigation
        if (source.getSubRefs() != null) {
            Set<String> subRefs = source.getSubRefs().stream()
                    .filter(sub -> sub != null && sub.getPath() != null)
                    .map(GroupEntity::getPath)
                    .collect(Collectors.toSet());
            target.setSubRefs(subRefs);

            Map<String, String> subRefIds = source.getSubRefs().stream()
                    .filter(sub -> sub != null && sub.getPath() != null && sub.getId() != null)
                    .collect(Collectors.toMap(GroupEntity::getPath, GroupEntity::getId));
            target.setSubRefIds(subRefIds);
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
