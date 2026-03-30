package de.ebusyness.priceproviderservice.facade.group.mapper;

import de.ebusyness.commons.mapper.AbstractMapper;
import de.ebusyness.commons.mapper.RestResponseMappingContext;
import de.ebusyness.commons.web.rest.InfoAuditableRestEntity;
import de.ebusyness.priceproviderservice.dataaccess.group.entity.GroupEntity;
import de.ebusyness.priceproviderservice.facade.group.restentity.GroupRestEntity;
import org.springframework.stereotype.Component;

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
        target.setName(source.getName());

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
