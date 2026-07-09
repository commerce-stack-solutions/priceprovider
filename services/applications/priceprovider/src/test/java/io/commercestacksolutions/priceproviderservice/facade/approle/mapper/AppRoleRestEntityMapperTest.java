package io.commercestacksolutions.priceproviderservice.facade.approle.mapper;

import io.commercestacksolutions.commons.mapper.RestResponseMappingContext;
import io.commercestacksolutions.commons.mapper.exception.DataMappingException;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppRoleEntity;
import io.commercestacksolutions.priceproviderservice.facade.approle.restentity.AppRoleRestEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link AppRoleRestEntityMapper}.
 *
 * <p>Verifies that basic fields are mapped correctly, that
 * {@code permissionRefs} is projected to names, and that
 * {@code $info.permissionRefIds} (name → id map) is always populated
 * for UI navigation links regardless of {@code $expand}.</p>
 */
class AppRoleRestEntityMapperTest {

    private AppRoleRestEntityMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new AppRoleRestEntityMapper();
    }

    // ---------- helpers ----------

    private static AppPermissionEntity permission(Long id, String name) {
        AppPermissionEntity p = new AppPermissionEntity();
        p.setId(id);
        p.setName(name);
        return p;
    }

    private static RestResponseMappingContext emptyContext() {
        return new RestResponseMappingContext();
    }

    private static RestResponseMappingContext infoContext() {
        RestResponseMappingContext ctx = new RestResponseMappingContext();
        ctx.addExpandPaths(Set.of("$info"));
        return ctx;
    }

    // ---------- basic field mapping ----------

    @Test
    void convert_basicFields_areMappedCorrectly() throws DataMappingException {
        AppRoleEntity source = new AppRoleEntity();
        source.setId(42L);
        source.setName("priceprovider.admin:Superuser");
        source.setDescription("Superuser role");

        AppRoleRestEntity target = mapper.convert(source, emptyContext());

        assertEquals(42L, target.getId());
        assertEquals("priceprovider.admin:Superuser", target.getName());
        assertEquals("Superuser role", target.getDescription());
    }

    // ---------- permissionRefs name projection ----------

    @Test
    void convert_withPermissions_permissionRefsContainsNames() throws DataMappingException {
        AppRoleEntity source = new AppRoleEntity();
        source.setId(1L);
        source.setName("role");
        source.setPermissionRefs(Set.of(
                permission(10L, "priceprovider.admin:ServiceInitialization:write"),
                permission(20L, "priceprovider.admin:PriceRow:read")
        ));

        AppRoleRestEntity target = mapper.convert(source, emptyContext());

        assertNotNull(target.getPermissionRefs());
        assertTrue(target.getPermissionRefs().contains("priceprovider.admin:ServiceInitialization:write"));
        assertTrue(target.getPermissionRefs().contains("priceprovider.admin:PriceRow:read"));
    }

    @Test
    void convert_noPermissions_permissionRefsIsEmpty() throws DataMappingException {
        AppRoleEntity source = new AppRoleEntity();
        source.setId(2L);
        source.setName("empty-role");

        AppRoleRestEntity target = mapper.convert(source, emptyContext());

        // permissionRefs should be an empty set (not null) from the default in the entity
        assertNotNull(target.getPermissionRefs());
        assertTrue(target.getPermissionRefs().isEmpty());
    }

    // ---------- permissionRefIds always populated ----------

    @Test
    void convert_withPermissions_infoPermissionRefIdsPopulatedWithoutExpand() throws DataMappingException {
        AppRoleEntity source = new AppRoleEntity();
        source.setId(1L);
        source.setName("role");
        source.setPermissionRefs(Set.of(
                permission(10L, "priceprovider.admin:ServiceInitialization:write"),
                permission(20L, "priceprovider.admin:PriceRow:read")
        ));

        // No $expand requested – permissionRefIds must still be present
        AppRoleRestEntity target = mapper.convert(source, emptyContext());

        assertNotNull(target.getInfo(), "$info must always be set");
        assertNotNull(target.getInfo().getPermissionRefIds(), "permissionRefIds must always be populated");
        assertEquals(10L, target.getInfo().getPermissionRefIds().get("priceprovider.admin:ServiceInitialization:write"));
        assertEquals(20L, target.getInfo().getPermissionRefIds().get("priceprovider.admin:PriceRow:read"));
    }

    @Test
    void convert_noPermissions_infoPermissionRefIdsIsNullOrEmpty() throws DataMappingException {
        AppRoleEntity source = new AppRoleEntity();
        source.setId(2L);
        source.setName("empty-role");

        AppRoleRestEntity target = mapper.convert(source, emptyContext());

        // When there are no permissions, permissionRefIds should be null (not populated)
        assertNotNull(target.getInfo(), "$info must always be set");
        assertNull(target.getInfo().getPermissionRefIds(),
                "permissionRefIds must be null when there are no permissions");
    }

    // ---------- audit timestamps only on $info expand ----------

    @Test
    void convert_withInfoExpand_auditTimestampsAreSet() throws DataMappingException {
        OffsetDateTime created = OffsetDateTime.now().minusDays(1);
        OffsetDateTime modified = OffsetDateTime.now();

        AppRoleEntity source = new AppRoleEntity();
        source.setId(3L);
        source.setName("role");
        source.setCreatedAt(created);
        source.setLastModifiedAt(modified);

        AppRoleRestEntity target = mapper.convert(source, infoContext());

        assertNotNull(target.getInfo());
        assertEquals(created, target.getInfo().getCreatedAt());
        assertEquals(modified, target.getInfo().getLastModifiedAt());
    }

    @Test
    void convert_withoutInfoExpand_auditTimestampsAreNull() throws DataMappingException {
        AppRoleEntity source = new AppRoleEntity();
        source.setId(4L);
        source.setName("role");
        source.setCreatedAt(OffsetDateTime.now());
        source.setLastModifiedAt(OffsetDateTime.now());

        AppRoleRestEntity target = mapper.convert(source, emptyContext());

        assertNotNull(target.getInfo());
        assertNull(target.getInfo().getCreatedAt());
        assertNull(target.getInfo().getLastModifiedAt());
    }
}
