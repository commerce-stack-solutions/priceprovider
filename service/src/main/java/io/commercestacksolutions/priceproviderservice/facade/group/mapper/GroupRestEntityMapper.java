package io.commercestacksolutions.priceproviderservice.facade.group.mapper;

import io.commercestacksolutions.commons.mapper.AbstractMapper;
import io.commercestacksolutions.commons.mapper.RestResponseMappingContext;
import io.commercestacksolutions.commons.web.rest.InfoAuditableRestEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.facade.group.restentity.GroupRestEntity;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class GroupRestEntityMapper extends AbstractMapper<GroupEntity, GroupRestEntity, RestResponseMappingContext> {

    @Override
    public GroupRestEntity createTarget() {
        return new GroupRestEntity();
    }

    @Override
    public void convert(GroupEntity source, GroupRestEntity target, RestResponseMappingContext context) {
        target.setId(source.getId());
        target.setPath(source.getPath());
        target.setName(source.getName());

        // Convert parent entities to paths and build path→UUID map for navigation
        if (source.getParentRefs() != null) {
            Set<String> parentRefs = source.getParentRefs().stream()
                    .filter(parent -> parent != null && parent.getPath() != null)
                    .map(GroupEntity::getPath)
                    .collect(Collectors.toSet());
            target.setParentRefs(parentRefs);

            Map<String, String> parentRefIds = source.getParentRefs().stream()
                    .filter(parent -> parent != null && parent.getPath() != null && parent.getId() != null)
                    .collect(Collectors.toMap(GroupEntity::getPath, p -> p.getId().toString()));
            target.setParentRefIds(parentRefIds);
        }

        // Convert sub entities to paths and build path→UUID map for navigation
        if (source.getSubRefs() != null) {
            Set<String> subRefs = source.getSubRefs().stream()
                    .filter(sub -> sub != null && sub.getPath() != null)
                    .map(GroupEntity::getPath)
                    .collect(Collectors.toSet());
            target.setSubRefs(subRefs);

            Map<String, String> subRefIds = source.getSubRefs().stream()
                    .filter(sub -> sub != null && sub.getPath() != null && sub.getId() != null)
                    .collect(Collectors.toMap(GroupEntity::getPath, s -> s.getId().toString()));
            target.setSubRefIds(subRefIds);
        }

        if (context.shouldExpand("$info")) {
            addInfoSection(source, target, context);
        }
    }

    private void addInfoSection(GroupEntity source, GroupRestEntity target, RestResponseMappingContext context) {
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
