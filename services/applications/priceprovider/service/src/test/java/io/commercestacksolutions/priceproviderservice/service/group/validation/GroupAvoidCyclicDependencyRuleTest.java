package io.commercestacksolutions.priceproviderservice.service.group.validation;

import io.commercestacksolutions.priceproviderservice.dataaccess.group.GroupEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.commons.web.rest.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests for GroupAvoidCyclicDependencyRule to ensure cyclic dependencies are detected correctly.
 * Tests the ValidationRule interface implementation.
 */
class GroupAvoidCyclicDependencyRuleTest {

    private GroupAvoidCyclicDependencyRule rule;
    private GroupEntityRepository mockRepository;

    @BeforeEach
    void setup() {
        mockRepository = Mockito.mock(GroupEntityRepository.class);
        rule = new GroupAvoidCyclicDependencyRule(mockRepository);
    }

    @Test
    void noParents_ShouldBeValid() {
        GroupEntity group = new GroupEntity("G-A");

        when(mockRepository.findAll()).thenReturn(new ArrayList<>());

        List<Message> errors = rule.validate(group);

        assertTrue(errors.isEmpty(), "Group with no parents should be valid");
    }

    @Test
    void validParent_ShouldBeValid() {
        GroupEntity parent = new GroupEntity("G-ROOT");
        GroupEntity group = new GroupEntity("G-A");
        group.setParentRefs(Set.of(parent));

        when(mockRepository.findAll()).thenReturn(List.of(parent));

        List<Message> errors = rule.validate(group);

        assertTrue(errors.isEmpty(), "Group with valid parent should be valid");
    }

    @Test
    void directSelfReference_ShouldBeInvalid() {
        GroupEntity group = new GroupEntity("G-A");
        group.setParentRefs(Set.of(group));

        when(mockRepository.findAll()).thenReturn(new ArrayList<>());

        List<Message> errors = rule.validate(group);

        assertFalse(errors.isEmpty(), "Group cannot have itself as parent");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessageKey().contains("cyclic"));
    }

    @Test
    void twoNodeCycle_ShouldBeInvalid() {
        // G-A → G-B → G-A (cycle)
        GroupEntity groupA = new GroupEntity("G-A");
        GroupEntity groupB = new GroupEntity("G-B");

        // Existing: G-B → G-A
        groupB.setParentRefs(Set.of(groupA));

        // Now try G-A → G-B (would create cycle)
        groupA.setParentRefs(Set.of(groupB));

        when(mockRepository.findAll()).thenReturn(List.of(groupB));

        List<Message> errors = rule.validate(groupA);

        assertFalse(errors.isEmpty(), "Should detect cycle when G-A → G-B and G-B → G-A");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessageKey().contains("cyclic"));
    }

    @Test
    void threeNodeCycle_ShouldBeInvalid() {
        // G-A → G-B → G-C → G-A (cycle)
        GroupEntity groupA = new GroupEntity("G-A");
        GroupEntity groupB = new GroupEntity("G-B");
        GroupEntity groupC = new GroupEntity("G-C");

        // Existing: G-B → G-C → G-A
        groupC.setParentRefs(Set.of(groupA));
        groupB.setParentRefs(Set.of(groupC));

        // Now try G-A → G-B (would create cycle)
        groupA.setParentRefs(Set.of(groupB));

        when(mockRepository.findAll()).thenReturn(List.of(groupB, groupC));

        List<Message> errors = rule.validate(groupA);

        assertFalse(errors.isEmpty(), "Should detect cycle when G-A → G-B → G-C → G-A");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessageKey().contains("cyclic"));
    }

    @Test
    void longChainNoCycle_ShouldBeValid() {
        // G-A → G-B → G-C → G-D (no cycle)
        GroupEntity groupD = new GroupEntity("G-D");
        GroupEntity groupC = new GroupEntity("G-C");
        GroupEntity groupB = new GroupEntity("G-B");
        GroupEntity groupA = new GroupEntity("G-A");

        groupC.setParentRefs(Set.of(groupD));
        groupB.setParentRefs(Set.of(groupC));
        groupA.setParentRefs(Set.of(groupB));

        when(mockRepository.findAll()).thenReturn(List.of(groupB, groupC));

        List<Message> errors = rule.validate(groupA);

        assertTrue(errors.isEmpty(), "Long chain without cycle should be valid");
    }

    @Test
    void nullGroup_ShouldBeValid() {
        when(mockRepository.findAll()).thenReturn(new ArrayList<>());

        List<Message> errors = rule.validate(null);

        assertTrue(errors.isEmpty(), "Null group should be considered valid (no validation possible)");
    }

    @Test
    void groupWithNullId_ShouldBeValid() {
        GroupEntity group = new GroupEntity();
        GroupEntity parent = new GroupEntity("G-ROOT");
        group.setParentRefs(Set.of(parent));

        when(mockRepository.findAll()).thenReturn(new ArrayList<>());

        List<Message> errors = rule.validate(group);

        assertTrue(errors.isEmpty(), "Group with null id should be considered valid (no validation possible)");
    }

    @Test
    void validationError_ShouldContainCorrectFields() {
        GroupEntity groupA = new GroupEntity("G-A");
        GroupEntity groupB = new GroupEntity("G-B");

        groupB.setParentRefs(Set.of(groupA));
        groupA.setParentRefs(Set.of(groupB));

        when(mockRepository.findAll()).thenReturn(List.of(groupB));

        List<Message> errors = rule.validate(groupA);

        assertFalse(errors.isEmpty());
        Message error = errors.get(0);
        assertEquals(Message.MessageType.ERROR, error.getType());
        assertNotNull(error.getMessageKey());
        assertTrue(error.getMessageKey().contains("cyclic"));
        assertNotNull(error.getParameters());
        assertTrue(error.getParameters().containsKey("groupId"));
        assertTrue(error.getParameters().containsKey("parentId"));
        assertTrue(error.getFields().contains("parentRefs"));
        assertTrue(error.getFields().contains("subRefs"));
    }
}
