package io.commercestacksolutions.priceproviderservice.facade.organization.mapper;

import io.commercestacksolutions.commons.mapper.RestRequestMappingContext;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.GroupEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.organization.entity.OrganizationEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.organization.type.OrganizationType;
import io.commercestacksolutions.priceproviderservice.facade.organization.restentity.OrganizationRestEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link OrganizationEntityMapper}.
 *
 * <p>Verifies that a {@link DataMappingException} is thrown when a parent/sub ref path
 * cannot be resolved from the repository, and that valid refs are mapped correctly.</p>
 */
@ExtendWith(MockitoExtension.class)
public class OrganizationEntityMapperTest {

    @Mock
    private GroupEntityRepository groupEntityRepository;

    private OrganizationEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new OrganizationEntityMapper();
        ReflectionTestUtils.setField(mapper, "groupEntityRepository", groupEntityRepository);
    }

    // ---------- helpers ----------

    private static GroupEntity groupEntity(String id, String path) {
        GroupEntity e = new GroupEntity();
        e.setId(id);
        e.setPath(path);
        e.setName("Group " + path);
        return e;
    }

    private static OrganizationRestEntity orgRestEntity(String path, Set<String> parentRefs, Set<String> subRefs) {
        OrganizationRestEntity re = new OrganizationRestEntity();
        re.setPath(path);
        re.setName("Org " + path);
        re.setOrganizationType("COMPANY");
        re.setParentRefs(parentRefs);
        re.setSubRefs(subRefs);
        return re;
    }

    // ---------- parentRefs error ----------

    @Test
    void convert_unresolvedParentRefPath_throwsDataMappingException() {
        OrganizationRestEntity source = orgRestEntity("ORG-PATH", Set.of("MISSING-PARENT"), null);
        OrganizationEntity target = new OrganizationEntity();

        when(groupEntityRepository.findByPath("MISSING-PARENT")).thenReturn(Optional.empty());

        DataMappingException ex = assertThrows(DataMappingException.class,
                () -> mapper.convert(source, target, new RestRequestMappingContext<>(null)));

        assertNotNull(ex.getMessage());
    }

    @Test
    void convert_validParentRefPath_resolvedSuccessfully() throws DataMappingException {
        GroupEntity parentGroup = groupEntity("UUID-PARENT", "PARENT-PATH");
        OrganizationRestEntity source = orgRestEntity("ORG-PATH", Set.of("PARENT-PATH"), null);
        OrganizationEntity target = new OrganizationEntity();
        target.setPath("ORG-PATH");

        when(groupEntityRepository.findByPath("PARENT-PATH")).thenReturn(Optional.of(parentGroup));

        mapper.convert(source, target, new RestRequestMappingContext<>(null));

        assertNotNull(target.getParentRefs());
        assertEquals(1, target.getParentRefs().size());
        assertTrue(target.getParentRefs().stream().anyMatch(p -> "UUID-PARENT".equals(p.getId())));
    }

    // ---------- subRefs error ----------

    @Test
    void convert_unresolvedSubRefPath_throwsDataMappingException() {
        OrganizationEntity target = new OrganizationEntity();
        target.setId("UUID-ORG");
        target.setPath("ORG-PATH");

        OrganizationRestEntity source = orgRestEntity("ORG-PATH", null, Set.of("MISSING-SUB"));

        when(groupEntityRepository.findByPath("MISSING-SUB")).thenReturn(Optional.empty());

        DataMappingException ex = assertThrows(DataMappingException.class,
                () -> mapper.convert(source, target, new RestRequestMappingContext<>("UUID-ORG")));

        assertNotNull(ex.getMessage());
    }

    @Test
    void convert_validSubRefPath_resolvedSuccessfully() throws DataMappingException {
        GroupEntity subGroup = groupEntity("UUID-SUB", "SUB-PATH");
        subGroup.setParentRefs(new HashSet<>());

        OrganizationEntity target = new OrganizationEntity();
        target.setId("UUID-ORG");
        target.setPath("ORG-PATH");

        OrganizationRestEntity source = orgRestEntity("ORG-PATH", null, Set.of("SUB-PATH"));

        when(groupEntityRepository.findByPath("SUB-PATH")).thenReturn(Optional.of(subGroup));

        mapper.convert(source, target, new RestRequestMappingContext<>("UUID-ORG"));

        assertNotNull(target.getSubRefs());
        assertEquals(1, target.getSubRefs().size());
        assertTrue(target.getSubRefs().stream().anyMatch(s -> "UUID-SUB".equals(s.getId())));
    }

    // ---------- id from context ----------

    @Test
    void convert_withIdInContext_setsIdOnTarget() throws DataMappingException {
        OrganizationRestEntity source = orgRestEntity("MY-ORG-PATH", null, null);
        OrganizationEntity target = new OrganizationEntity();

        mapper.convert(source, target, new RestRequestMappingContext<>("PROVIDED-ID"));

        assertEquals("PROVIDED-ID", target.getId());
    }

    @Test
    void convert_withNullIdInContext_doesNotOverrideExistingId() throws DataMappingException {
        OrganizationEntity target = new OrganizationEntity();
        target.setId("EXISTING-ID");
        target.setPath("MY-ORG-PATH");

        OrganizationRestEntity source = orgRestEntity("MY-ORG-PATH", null, null);

        mapper.convert(source, target, new RestRequestMappingContext<>(null));

        assertEquals("EXISTING-ID", target.getId());
    }
}
