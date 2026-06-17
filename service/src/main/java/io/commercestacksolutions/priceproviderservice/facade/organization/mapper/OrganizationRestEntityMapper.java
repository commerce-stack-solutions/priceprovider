package io.commercestacksolutions.priceproviderservice.facade.organization.mapper;

import io.commercestacksolutions.commons.mapper.AbstractMapper;
import io.commercestacksolutions.commons.mapper.RestResponseMappingContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.organization.entity.OrganizationEntity;
import io.commercestacksolutions.priceproviderservice.facade.organization.info.InfoOrganization;
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
        target.setOrganizationType(source.getOrganizationType() != null ? source.getOrganizationType().code() : null);

        // Convert parent entities to paths
        if (source.getParentRefs() != null) {
            Set<String> parentRefs = source.getParentRefs().stream()
                    .filter(parent -> parent != null && parent.getPath() != null)
                    .map(GroupEntity::getPath)
                    .collect(Collectors.toSet());
            target.setParentRefs(parentRefs);
        }

        // Convert sub entities to paths
        if (source.getSubRefs() != null) {
            Set<String> subRefs = source.getSubRefs().stream()
                    .filter(sub -> sub != null && sub.getPath() != null)
                    .map(GroupEntity::getPath)
                    .collect(Collectors.toSet());
            target.setSubRefs(subRefs);
        }

        // Always populate $info with parentRefIds/subRefIds for navigation links
        // (audit timestamps are added only when $info is explicitly requested)
        addInfo(source, target, context);
    }

    private void addInfo(OrganizationEntity source, OrganizationRestEntity target, RestResponseMappingContext context) {
        InfoOrganization info = new InfoOrganization();

        // Always populate parentRefIds in $info for UI navigation links (path → id map)
        if (source.getParentRefs() != null) {
            Map<String, String> parentRefIds = source.getParentRefs().stream()
                    .filter(parent -> parent != null && parent.getPath() != null && parent.getId() != null)
                    .collect(Collectors.toMap(GroupEntity::getPath, GroupEntity::getId));
            if (!parentRefIds.isEmpty()) {
                info.setParentRefIds(parentRefIds);
            }
        }

        // Always populate subRefIds in $info for UI navigation links (path → id map)
        if (source.getSubRefs() != null) {
            Map<String, String> subRefIds = source.getSubRefs().stream()
                    .filter(sub -> sub != null && sub.getPath() != null && sub.getId() != null)
                    .collect(Collectors.toMap(GroupEntity::getPath, GroupEntity::getId));
            if (!subRefIds.isEmpty()) {
                info.setSubRefIds(subRefIds);
            }
        }

        // Add audit timestamps to $info when requested
        if (context.expandWithAnyOf(new String[]{"$info", "$info.createdAt"})) {
            info.setCreatedAt(source.getCreatedAt());
        }
        if (context.expandWithAnyOf(new String[]{"$info", "$info.lastModifiedAt"})) {
            info.setLastModifiedAt(source.getLastModifiedAt());
        }

        target.setInfo(info);
    }
}
