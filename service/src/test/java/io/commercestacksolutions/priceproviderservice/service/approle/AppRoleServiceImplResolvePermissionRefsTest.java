package io.commercestacksolutions.priceproviderservice.service.approle;

import io.commercestacksolutions.commons.service.entity.authorization.EntityAuthorizationService;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.AppPermissionEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.AppRoleEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppRoleEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AppRoleServiceImpl#resolvePermissionRefs(AppRoleEntity)}.
 *
 * <p>The key behaviour under test is that transient {@link AppPermissionEntity} stubs
 * are removed from the role's {@code permissionRefs} collection <em>before</em> any
 * JPQL query is issued.  This prevents Hibernate's auto-flush from throwing a
 * {@code TransientObjectException} when the method is called inside a long-running
 * transaction (e.g. from {@code SetupDataImportManager#loadDataAsync}).</p>
 */
@ExtendWith(MockitoExtension.class)
public class AppRoleServiceImplResolvePermissionRefsTest {

    @Mock
    private AppRoleEntityRepository appRoleEntityRepository;

    @Mock
    private AppPermissionEntityRepository appPermissionEntityRepository;

    @Mock
    private AuthorizationContext authorizationContext;

    @Mock
    private EntityAuthorizationService entityAuthorizationService;

    @Mock
    private EntityManager entityManager;

    @Mock
    private EntityManagerFactory entityManagerFactory;

    @Mock
    private EntityManager tempEntityManager;

    private AppRoleServiceImpl appRoleService;

    @BeforeEach
    void setUp() {
        appRoleService = new AppRoleServiceImpl(
                appRoleEntityRepository,
                appPermissionEntityRepository,
                List.of(),
                authorizationContext,
                entityAuthorizationService,
                entityManager
        );

        doNothing().when(entityAuthorizationService).checkAccessBeforeAndAfter(any(), any(), any(), any(), any());

        lenient().when(entityManager.getEntityManagerFactory()).thenReturn(entityManagerFactory);
        lenient().when(entityManagerFactory.createEntityManager()).thenReturn(tempEntityManager);
        lenient().when(tempEntityManager.find(eq(AppRoleEntity.class), any())).thenReturn(null);
        lenient().when(tempEntityManager.isOpen()).thenReturn(true);
        lenient().doNothing().when(tempEntityManager).close();
    }

    // ---------- helpers ----------

    private static AppPermissionEntity managedPermission(Long id, String name) {
        AppPermissionEntity p = new AppPermissionEntity();
        p.setId(id);
        p.setName(name);
        return p;
    }

    private static AppPermissionEntity transientStubByName(String name) {
        AppPermissionEntity stub = new AppPermissionEntity();
        stub.setName(name);
        // no id — transient
        return stub;
    }

    private static AppPermissionEntity transientStubById(Long id) {
        AppPermissionEntity stub = new AppPermissionEntity();
        stub.setId(id);
        return stub;
    }

    private static AppRoleEntity roleWithPermissions(Set<AppPermissionEntity> perms) {
        AppRoleEntity role = new AppRoleEntity();
        role.setId(1L);
        role.setName("priceprovider.admin:Superuser");
        role.setPermissionRefs(perms);
        return role;
    }

    // ---------- name-based resolution ----------

    @Test
    void save_permissionRefByName_isResolvedToManagedEntity() throws Exception {
        AppPermissionEntity managed = managedPermission(10L, "priceprovider.admin:AppRole:read");
        AppRoleEntity role = roleWithPermissions(Set.of(transientStubByName("priceprovider.admin:AppRole:read")));

        when(appPermissionEntityRepository.findByName("priceprovider.admin:AppRole:read"))
                .thenReturn(Optional.of(managed));
        when(appRoleEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appRoleService.save(role);

        verify(appPermissionEntityRepository).findByName("priceprovider.admin:AppRole:read");
        assertEquals(1, role.getPermissionRefs().size());
        assertTrue(role.getPermissionRefs().contains(managed),
                "permissionRefs must contain the managed entity after resolution");
    }

    @Test
    void save_permissionRefById_isResolvedToManagedEntity() throws Exception {
        AppPermissionEntity managed = managedPermission(20L, "priceprovider.admin:AppRole:write");
        AppRoleEntity role = roleWithPermissions(Set.of(transientStubById(20L)));

        when(appPermissionEntityRepository.findById(20L)).thenReturn(Optional.of(managed));
        when(appRoleEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appRoleService.save(role);

        verify(appPermissionEntityRepository).findById(20L);
        assertEquals(1, role.getPermissionRefs().size());
        assertTrue(role.getPermissionRefs().contains(managed));
    }

    @Test
    void save_permissionRefUnresolvable_isDroppedSilently() throws Exception {
        AppRoleEntity role = roleWithPermissions(Set.of(transientStubByName("priceprovider.admin:Missing:read")));

        when(appPermissionEntityRepository.findByName("priceprovider.admin:Missing:read"))
                .thenReturn(Optional.empty());
        when(appRoleEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appRoleService.save(role);

        assertTrue(role.getPermissionRefs().isEmpty(),
                "Unresolvable stub must be silently dropped");
    }

    @Test
    void save_emptyPermissionRefs_noRepositoryLookup() throws Exception {
        AppRoleEntity role = roleWithPermissions(new HashSet<>());

        when(appRoleEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appRoleService.save(role);

        verify(appPermissionEntityRepository, never()).findByName(any());
        verify(appPermissionEntityRepository, never()).findById(any());
    }

    @Test
    void save_nullPermissionRefs_noRepositoryLookup() throws Exception {
        AppRoleEntity role = roleWithPermissions(null);

        when(appRoleEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertDoesNotThrow(() -> appRoleService.save(role));
        verify(appPermissionEntityRepository, never()).findByName(any());
        verify(appPermissionEntityRepository, never()).findById(any());
    }

    /**
     * Regression test for the bug reported in issue #76.
     *
     * <p>When {@code resolvePermissionRefs} is called inside a long-running transaction
     * the role entity may already be <em>managed</em> by the JPA persistence context.
     * Setting transient permission stubs on it and then executing a JPQL query triggers
     * Hibernate's auto-flush.  If the stubs are still attached to the entity during the
     * flush, Hibernate throws {@code TransientObjectException}.</p>
     *
     * <p>This test verifies that the permissionRefs collection is cleared on the entity
     * <em>before</em> the first {@code findByName} call, so that any auto-flush sees a
     * clean (empty) collection.</p>
     */
    @Test
    void resolvePermissionRefs_clearsTransientRefsBeforeQuery() throws Exception {
        AppPermissionEntity managed = managedPermission(30L, "priceprovider.admin:Superuser:write");
        AppPermissionEntity transientStub = transientStubByName("priceprovider.admin:Superuser:write");

        AppRoleEntity role = roleWithPermissions(new HashSet<>(Set.of(transientStub)));

        AtomicInteger sizeAtQueryTime = new AtomicInteger(-1);

        when(appPermissionEntityRepository.findByName("priceprovider.admin:Superuser:write"))
                .thenAnswer(inv -> {
                    // Capture the permissionRefs size at the moment the query is issued.
                    // With the fix applied, this must be 0 (stubs already cleared).
                    sizeAtQueryTime.set(role.getPermissionRefs().size());
                    return Optional.of(managed);
                });
        when(appRoleEntityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        appRoleService.save(role);

        assertEquals(0, sizeAtQueryTime.get(),
                "permissionRefs must be empty at the time findByName is called " +
                "(transient stubs must be cleared before any query to prevent Hibernate auto-flush issues)");

        assertEquals(1, role.getPermissionRefs().size());
        assertTrue(role.getPermissionRefs().contains(managed));
    }
}
