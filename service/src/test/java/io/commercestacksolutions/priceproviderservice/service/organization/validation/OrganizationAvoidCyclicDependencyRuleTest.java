package io.commercestacksolutions.priceproviderservice.service.organization.validation;

import io.commercestacksolutions.priceproviderservice.dataaccess.group.GroupEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.organization.entity.OrganizationEntity;
import io.commercestacksolutions.commons.web.rest.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests for OrganizationAvoidCyclicDependencyRule to ensure cyclic dependencies are detected correctly.
 * Tests the ValidationRule interface implementation.
 */
class OrganizationAvoidCyclicDependencyRuleTest {

    private OrganizationAvoidCyclicDependencyRule rule;
    private GroupEntityRepository mockRepository;

    @BeforeEach
    void setup() {
        mockRepository = Mockito.mock(GroupEntityRepository.class);
        rule = new OrganizationAvoidCyclicDependencyRule(mockRepository);
    }

    @Test
    void noParents_ShouldBeValid() {
        OrganizationEntity org = new OrganizationEntity("ORG-A");

        when(mockRepository.findAll()).thenReturn(new ArrayList<>());

        List<Message> errors = rule.validate(org);

        assertTrue(errors.isEmpty(), "Organization with no parents should be valid");
    }

    @Test
    void validParent_ShouldBeValid() {
        GroupEntity parent = new GroupEntity("GRP-ROOT");
        OrganizationEntity org = new OrganizationEntity("ORG-A");
        org.setParentRefs(Set.of(parent));

        when(mockRepository.findAll()).thenReturn(List.of(parent));

        List<Message> errors = rule.validate(org);

        assertTrue(errors.isEmpty(), "Organization with valid parent should be valid");
    }

    @Test
    void directSelfReference_ShouldBeInvalid() {
        OrganizationEntity org = new OrganizationEntity("ORG-A");
        org.setParentRefs(Set.of(org));

        when(mockRepository.findAll()).thenReturn(new ArrayList<>());

        List<Message> errors = rule.validate(org);

        assertFalse(errors.isEmpty(), "Organization cannot have itself as parent");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessageKey().contains("cyclic"));
    }

    @Test
    void twoNodeCycle_ShouldBeInvalid() {
        // ORG-A → ORG-B → ORG-A (cycle)
        OrganizationEntity orgA = new OrganizationEntity("ORG-A");
        OrganizationEntity orgB = new OrganizationEntity("ORG-B");

        // Existing: ORG-B → ORG-A
        orgB.setParentRefs(Set.of(orgA));

        // Now try ORG-A → ORG-B (would create cycle)
        orgA.setParentRefs(Set.of(orgB));

        when(mockRepository.findAll()).thenReturn(List.of(orgB));

        List<Message> errors = rule.validate(orgA);

        assertFalse(errors.isEmpty(), "Should detect cycle when ORG-A → ORG-B and ORG-B → ORG-A");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessageKey().contains("cyclic"));
    }

    @Test
    void threeNodeCycle_ShouldBeInvalid() {
        // ORG-A → ORG-B → ORG-C → ORG-A (cycle)
        OrganizationEntity orgA = new OrganizationEntity("ORG-A");
        OrganizationEntity orgB = new OrganizationEntity("ORG-B");
        OrganizationEntity orgC = new OrganizationEntity("ORG-C");

        // Existing: ORG-B → ORG-C → ORG-A
        orgC.setParentRefs(Set.of(orgA));
        orgB.setParentRefs(Set.of(orgC));

        // Now try ORG-A → ORG-B (would create cycle)
        orgA.setParentRefs(Set.of(orgB));

        when(mockRepository.findAll()).thenReturn(List.of(orgB, orgC));

        List<Message> errors = rule.validate(orgA);

        assertFalse(errors.isEmpty(), "Should detect cycle when ORG-A → ORG-B → ORG-C → ORG-A");
        assertEquals(1, errors.size());
        assertTrue(errors.get(0).getMessageKey().contains("cyclic"));
    }

    @Test
    void longChainNoCycle_ShouldBeValid() {
        // ORG-A → ORG-B → ORG-C → ORG-D (no cycle)
        OrganizationEntity orgD = new OrganizationEntity("ORG-D");
        OrganizationEntity orgC = new OrganizationEntity("ORG-C");
        OrganizationEntity orgB = new OrganizationEntity("ORG-B");
        OrganizationEntity orgA = new OrganizationEntity("ORG-A");

        orgC.setParentRefs(Set.of(orgD));
        orgB.setParentRefs(Set.of(orgC));
        orgA.setParentRefs(Set.of(orgB));

        when(mockRepository.findAll()).thenReturn(List.of(orgB, orgC));

        List<Message> errors = rule.validate(orgA);

        assertTrue(errors.isEmpty(), "Long chain without cycle should be valid");
    }

    @Test
    void nullOrganization_ShouldBeValid() {
        when(mockRepository.findAll()).thenReturn(new ArrayList<>());

        List<Message> errors = rule.validate(null);

        assertTrue(errors.isEmpty(), "Null organization should be considered valid (no validation possible)");
    }

    @Test
    void organizationWithNullId_ShouldBeValid() {
        OrganizationEntity org = new OrganizationEntity();
        GroupEntity parent = new GroupEntity("GRP-ROOT");
        org.setParentRefs(Set.of(parent));

        when(mockRepository.findAll()).thenReturn(new ArrayList<>());

        List<Message> errors = rule.validate(org);

        assertTrue(errors.isEmpty(), "Organization with null id should be considered valid (no validation possible)");
    }

    @Test
    void validationError_ShouldContainCorrectFields() {
        OrganizationEntity orgA = new OrganizationEntity("ORG-A");
        OrganizationEntity orgB = new OrganizationEntity("ORG-B");

        orgB.setParentRefs(Set.of(orgA));
        orgA.setParentRefs(Set.of(orgB));

        when(mockRepository.findAll()).thenReturn(List.of(orgB));

        List<Message> errors = rule.validate(orgA);

        assertFalse(errors.isEmpty());
        Message error = errors.get(0);
        assertEquals(Message.MessageType.ERROR, error.getType());
        assertNotNull(error.getMessageKey());
        assertTrue(error.getMessageKey().contains("cyclic"));
        assertNotNull(error.getParameters());
        assertTrue(error.getParameters().containsKey("organizationId"));
        assertTrue(error.getParameters().containsKey("parentId"));
        assertTrue(error.getFields().contains("parentRefs"));
        assertTrue(error.getFields().contains("subRefs"));
    }
}
