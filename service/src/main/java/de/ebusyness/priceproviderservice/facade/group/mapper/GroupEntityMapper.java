package de.ebusyness.priceproviderservice.facade.group.mapper;

import de.ebusyness.commons.mapper.AbstractMapper;
import de.ebusyness.commons.mapper.RestRequestMappingContext;
import de.ebusyness.commons.mapper.exception.DataMappingException;
import de.ebusyness.priceproviderservice.dataaccess.group.entity.GroupEntity;
import de.ebusyness.priceproviderservice.dataaccess.group.GroupEntityRepository;
import de.ebusyness.priceproviderservice.facade.group.restentity.GroupRestEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

@Component
public class GroupEntityMapper extends AbstractMapper<GroupRestEntity, GroupEntity, RestRequestMappingContext<String>> {

    @Autowired
    private GroupEntityRepository groupEntityRepository;

    @Override
    public GroupEntity createTarget() {
        return new GroupEntity();
    }

    @Override
    public void convert(GroupRestEntity source, GroupEntity target, RestRequestMappingContext<String> context) throws DataMappingException {
        target.setId(context.getId());
        target.setName(source.getName());

        // Convert parent IDs to entity references
        if (source.getParentRefs() != null) {
            Set<GroupEntity> parentRefs = new HashSet<>();
            for (String parentId : source.getParentRefs()) {
                GroupEntity parent = new GroupEntity();
                parent.setId(parentId);
                parentRefs.add(parent);
            }
            target.setParentRefs(parentRefs);
        }

        // Convert sub IDs to entity references
        // Since 'subRefs' is the inverse side of the bidirectional relationship (mappedBy="parentRefs"),
        // we need to update the 'parentRefs' field of the sub entities to persist the relationship
        if (source.getSubRefs() != null) {
            Set<String> newSubIds = source.getSubRefs();
            Set<String> currentSubIds = new HashSet<>();
            
            // Collect current sub IDs
            if (target.getSubRefs() != null) {
                for (GroupEntity oldSub : target.getSubRefs()) {
                    currentSubIds.add(oldSub.getId());
                }
            }
            
            // Remove relationships for subs that are no longer in the list
            for (String oldSubId : currentSubIds) {
                if (!newSubIds.contains(oldSubId)) {
                    GroupEntity oldSub = groupEntityRepository.findById(oldSubId).orElse(null);
                    if (oldSub != null) {
                        // Remove by ID to avoid instance comparison issues
                        oldSub.getParentRefs().removeIf(p -> p.getId().equals(target.getId()));
                    }
                }
            }
            
            // Add new relationships
            Set<GroupEntity> subRefs = new HashSet<>();
            for (String subId : newSubIds) {
                GroupEntity sub = groupEntityRepository.findById(subId).orElse(null);
                if (sub != null) {
                    subRefs.add(sub);
                    // Only add if not already present (check by ID, not instance)
                    boolean alreadyExists = sub.getParentRefs().stream()
                        .anyMatch(p -> p.getId().equals(target.getId()));
                    if (!alreadyExists) {
                        sub.getParentRefs().add(target);
                    }
                }
            }
            target.setSubRefs(subRefs);
        }
    }
}
