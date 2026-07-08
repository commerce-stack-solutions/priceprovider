package io.commercestacksolutions.priceproviderservice.facade.group.mapper;

import io.commercestacksolutions.commons.mapper.RestRequestMappingContext;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.GroupEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.facade.group.restentity.GroupRestEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link GroupEntityMapper}.
 *
 * <p>Verifies that a {@link DataMappingException} is thrown when a parent/sub ref path
 * cannot be resolved from the repository, and that valid refs are mapped correctly.</p>
 */
@ExtendWith(MockitoExtension.class)
public class GroupEntityMapperTest {

    @Mock
    private GroupEntityRepository groupEntityRepository;

    private GroupEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new GroupEntityMapper();
        ReflectionTestUtils.setField(mapper, "groupEntityRepository", groupEntityRepository);
    }

    // ---------- helpers ----------

    private static GroupEntity fullEntity(String id, String path) {
        GroupEntity e = new GroupEntity();
        e.setId(id);
        e.setPath(path);
        e.setName("Name " + path);
        return e;
    }

    private static GroupRestEntity restEntity(String path, Set<String> parentRefs, Set<String> subRefs) {
        GroupRestEntity re = new GroupRestEntity();
        re.setPath(path);
        re.setName("Name " + path);
        re.setParentRefs(parentRefs);
        re.setSubRefs(subRefs);
        return re;
    }

    // ---------- parentRefs error ----------

    @Test
    void convert_unresolvedParentRefPath_throwsDataMappingException() {
        GroupRestEntity source = restEntity("CHILD-PATH", Set.of("MISSING-PARENT"), null);
        GroupEntity target = new GroupEntity();

        when(groupEntityRepository.findByPath("MISSING-PARENT")).thenReturn(Optional.empty());

        DataMappingException ex = assertThrows(DataMappingException.class,
                () -> mapper.convert(source, target, new RestRequestMappingContext<>(null)));

        assertNotNull(ex.getMessage());
    }

    @Test
    void convert_validParentRefPath_resolvedSuccessfully() throws DataMappingException {
        GroupEntity parentEntity = fullEntity("UUID-PARENT", "PARENT-PATH");
        GroupRestEntity source = restEntity("CHILD-PATH", Set.of("PARENT-PATH"), null);
        GroupEntity target = new GroupEntity();
        target.setPath("CHILD-PATH");

        when(groupEntityRepository.findByPath("PARENT-PATH")).thenReturn(Optional.of(parentEntity));

        mapper.convert(source, target, new RestRequestMappingContext<>(null));

        assertNotNull(target.getParentRefs());
        assertEquals(1, target.getParentRefs().size());
        assertTrue(target.getParentRefs().stream().anyMatch(p -> "UUID-PARENT".equals(p.getId())));
    }

    // ---------- subRefs error ----------

    @Test
    void convert_unresolvedSubRefPath_throwsDataMappingException() {
        GroupEntity target = fullEntity("UUID-PARENT", "PARENT-PATH");

        GroupRestEntity source = restEntity("PARENT-PATH", null, Set.of("MISSING-SUB"));

        when(groupEntityRepository.findByPath("MISSING-SUB")).thenReturn(Optional.empty());

        DataMappingException ex = assertThrows(DataMappingException.class,
                () -> mapper.convert(source, target, new RestRequestMappingContext<>("UUID-PARENT")));

        assertNotNull(ex.getMessage());
    }

    @Test
    void convert_validSubRefPath_resolvedSuccessfully() throws DataMappingException {
        GroupEntity subEntity = fullEntity("UUID-SUB", "SUB-PATH");
        subEntity.setParentRefs(new java.util.HashSet<>());
        GroupEntity target = fullEntity("UUID-PARENT", "PARENT-PATH");

        GroupRestEntity source = restEntity("PARENT-PATH", null, Set.of("SUB-PATH"));

        when(groupEntityRepository.findByPath("SUB-PATH")).thenReturn(Optional.of(subEntity));

        mapper.convert(source, target, new RestRequestMappingContext<>("UUID-PARENT"));

        assertNotNull(target.getSubRefs());
        assertEquals(1, target.getSubRefs().size());
        assertTrue(target.getSubRefs().stream().anyMatch(s -> "UUID-SUB".equals(s.getId())));
    }

    // ---------- id from context ----------

    @Test
    void convert_withIdInContext_setsIdOnTarget() throws DataMappingException {
        GroupRestEntity source = restEntity("MY-PATH", null, null);
        GroupEntity target = new GroupEntity();

        mapper.convert(source, target, new RestRequestMappingContext<>("PROVIDED-ID"));

        assertEquals("PROVIDED-ID", target.getId());
    }

    @Test
    void convert_withNullIdInContext_doesNotOverrideExistingId() throws DataMappingException {
        GroupEntity target = fullEntity("EXISTING-ID", "MY-PATH");
        GroupRestEntity source = restEntity("MY-PATH", null, null);

        mapper.convert(source, target, new RestRequestMappingContext<>(null));

        // null context id must not wipe the existing id on target
        assertEquals("EXISTING-ID", target.getId());
    }
}
