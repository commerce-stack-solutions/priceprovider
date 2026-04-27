package io.commercestacksolutions.priceproviderservice.service.group;

import io.commercestacksolutions.commons.permissionselector.SpecificationCombiner;
import io.commercestacksolutions.commons.service.entity.authorization.EntityAuthorizationService;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.GroupEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GroupServiceImpl#resolvePathBasedRefs(GroupEntity)}.
 *
 * <p>Verifies that both {@code parentRefs} and {@code subRefs} are resolved from path-only
 * stubs to fully loaded entities fetched from the repository.</p>
 */
@ExtendWith(MockitoExtension.class)
public class GroupServiceImplResolveRefsTest {

    @Mock
    private GroupEntityRepository groupEntityRepository;

    @SuppressWarnings("unchecked")
    @Mock
    private ValidationRule<GroupEntity> validationRule;

    @Mock
    private SpecificationCombiner specificationCombiner;

    @Mock
    private AuthorizationContext authorizationContext;

    @Mock
    private EntityAuthorizationService entityAuthorizationService;

    private GroupServiceImpl groupService;

    @BeforeEach
    void setUp() {
        groupService = new GroupServiceImpl(groupEntityRepository, List.of(validationRule), specificationCombiner, authorizationContext, entityAuthorizationService);
    }

    // ---------- helpers ----------

    private static GroupEntity fullEntity(String id, String path) {
        GroupEntity e = new GroupEntity();
        e.setId(id);
        e.setPath(path);
        e.setName("Name of " + path);
        return e;
    }

    private static GroupEntity pathOnlyStub(String path) {
        GroupEntity stub = new GroupEntity();
        stub.setPath(path);
        return stub;
    }

    // ---------- parentRefs ----------

    @Test
    void save_parentRefWithIdOnly_passedThroughUnchanged() throws Exception {
        GroupEntity parent = fullEntity("UUID-PARENT", "PARENT-PATH");
        GroupEntity child = fullEntity("UUID-CHILD", "CHILD-PATH");
        child.setName("Child");
        child.setParentRefs(Set.of(parent));

        when(groupEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(validationRule.validate(any())).thenReturn(List.of());

        groupService.save(child);

        // Repository must NOT be asked to look up parent by path because id is already set
        verify(groupEntityRepository, never()).findByPath("PARENT-PATH");
    }

    @Test
    void save_parentRefWithPathOnly_isResolvedFromRepository() throws Exception {
        GroupEntity resolvedParent = fullEntity("UUID-PARENT", "PARENT-PATH");
        GroupEntity child = fullEntity("UUID-CHILD", "CHILD-PATH");
        child.setName("Child");
        child.setParentRefs(Set.of(pathOnlyStub("PARENT-PATH")));

        when(groupEntityRepository.findByPath("PARENT-PATH")).thenReturn(Optional.of(resolvedParent));
        when(groupEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(validationRule.validate(any())).thenReturn(List.of());

        groupService.save(child);

        verify(groupEntityRepository).findByPath("PARENT-PATH");
        // Verify the resolved entity is in parentRefs after save
        assertEquals(1, child.getParentRefs().size());
        assertTrue(child.getParentRefs().stream().anyMatch(p -> "UUID-PARENT".equals(p.getId())),
                "parentRefs must contain the resolved entity with a real id");
    }

    @Test
    void save_parentRefWithUnresolvablePath_silentlyDropped() throws Exception {
        GroupEntity child = fullEntity("UUID-CHILD", "CHILD-PATH");
        child.setName("Child");
        child.setParentRefs(Set.of(pathOnlyStub("NON-EXISTENT")));

        when(groupEntityRepository.findByPath("NON-EXISTENT")).thenReturn(Optional.empty());
        when(groupEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(validationRule.validate(any())).thenReturn(List.of());

        groupService.save(child);

        // When path cannot be resolved, the stub is dropped from parentRefs
        assertTrue(child.getParentRefs().isEmpty(),
                "Unresolvable parentRef stub must be dropped silently");
    }

    // ---------- subRefs ----------

    @Test
    void save_subRefWithIdOnly_passedThroughUnchanged() throws Exception {
        GroupEntity sub = fullEntity("UUID-SUB", "SUB-PATH");
        GroupEntity parent = fullEntity("UUID-PARENT", "PARENT-PATH");
        parent.setName("Parent");
        parent.setSubRefs(Set.of(sub));

        when(groupEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(validationRule.validate(any())).thenReturn(List.of());

        groupService.save(parent);

        verify(groupEntityRepository, never()).findByPath("SUB-PATH");
    }

    @Test
    void save_subRefWithPathOnly_isResolvedFromRepository() throws Exception {
        GroupEntity resolvedSub = fullEntity("UUID-SUB", "SUB-PATH");
        GroupEntity parent = fullEntity("UUID-PARENT", "PARENT-PATH");
        parent.setName("Parent");
        parent.setSubRefs(Set.of(pathOnlyStub("SUB-PATH")));

        when(groupEntityRepository.findByPath("SUB-PATH")).thenReturn(Optional.of(resolvedSub));
        when(groupEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(validationRule.validate(any())).thenReturn(List.of());

        groupService.save(parent);

        verify(groupEntityRepository).findByPath("SUB-PATH");
        assertEquals(1, parent.getSubRefs().size());
        assertTrue(parent.getSubRefs().stream().anyMatch(s -> "UUID-SUB".equals(s.getId())),
                "subRefs must contain the resolved entity with a real id");
    }

    @Test
    void save_subRefWithUnresolvablePath_silentlyDropped() throws Exception {
        GroupEntity parent = fullEntity("UUID-PARENT", "PARENT-PATH");
        parent.setName("Parent");
        parent.setSubRefs(Set.of(pathOnlyStub("NON-EXISTENT-SUB")));

        when(groupEntityRepository.findByPath("NON-EXISTENT-SUB")).thenReturn(Optional.empty());
        when(groupEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(validationRule.validate(any())).thenReturn(List.of());

        groupService.save(parent);

        assertTrue(parent.getSubRefs().isEmpty(),
                "Unresolvable subRef stub must be dropped silently");
    }

    // ---------- null / empty guards ----------

    @Test
    void save_nullParentRefs_doesNotThrow() throws Exception {
        GroupEntity entity = fullEntity("UUID-1", "PATH-1");
        entity.setName("Entity");
        entity.setParentRefs(null);

        when(groupEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(validationRule.validate(any())).thenReturn(List.of());

        assertDoesNotThrow(() -> groupService.save(entity));
    }

    @Test
    void save_nullSubRefs_doesNotThrow() throws Exception {
        GroupEntity entity = fullEntity("UUID-1", "PATH-1");
        entity.setName("Entity");
        entity.setSubRefs(null);

        when(groupEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(validationRule.validate(any())).thenReturn(List.of());

        assertDoesNotThrow(() -> groupService.save(entity));
    }

    @Test
    void save_bothParentAndSubRefsPathOnly_bothAreResolved() throws Exception {
        GroupEntity resolvedParent = fullEntity("UUID-PARENT", "PARENT-PATH");
        GroupEntity resolvedSub = fullEntity("UUID-SUB", "SUB-PATH");

        GroupEntity entity = fullEntity("UUID-ENTITY", "ENTITY-PATH");
        entity.setName("Entity");
        entity.setParentRefs(Set.of(pathOnlyStub("PARENT-PATH")));
        entity.setSubRefs(Set.of(pathOnlyStub("SUB-PATH")));

        when(groupEntityRepository.findByPath("PARENT-PATH")).thenReturn(Optional.of(resolvedParent));
        when(groupEntityRepository.findByPath("SUB-PATH")).thenReturn(Optional.of(resolvedSub));
        when(groupEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(validationRule.validate(any())).thenReturn(List.of());

        groupService.save(entity);

        verify(groupEntityRepository).findByPath("PARENT-PATH");
        verify(groupEntityRepository).findByPath("SUB-PATH");
        assertEquals(1, entity.getParentRefs().size());
        assertEquals(1, entity.getSubRefs().size());
    }
}
