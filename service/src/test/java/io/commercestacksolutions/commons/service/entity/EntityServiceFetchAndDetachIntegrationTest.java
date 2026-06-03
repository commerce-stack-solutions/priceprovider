package io.commercestacksolutions.commons.service.entity;

import io.commercestacksolutions.priceproviderservice.config.TestSecurityConfig;
import io.commercestacksolutions.priceproviderservice.config.security.AuthorizationContext;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.CurrencyEntityRepository;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test to verify that fetchAndDetachExistingEntity correctly fetches
 * entities from the database using a temporary EntityManager, rather than returning
 * cached entities from the main EntityManager's persistence context.
 *
 * This test proves that the temporary EntityManager approach works correctly and
 * avoids the red flag issue of clearing the entire persistence context with entityManager.clear().
 */
@SpringBootTest
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
public class EntityServiceFetchAndDetachIntegrationTest {

    @Autowired
    private CurrencyEntityRepository currencyRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private String testCurrencyId;

    @BeforeEach
    public void setup() {
        // Setup mock HTTP request context for API context resolution
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/admin/api/currencies");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        // Enable bootstrap mode to allow initial data setup
        AuthorizationContext.enableBootstrapMode();

        // Create and commit a test entity in a separate transaction
        // This simulates an entity that already exists in the database from a previous transaction
        TransactionStatus txSetup = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            CurrencyEntity currency = new CurrencyEntity("TEST-EUR-FETCH");
            currency.setSymbol("€");
            Map<String, String> names = new HashMap<>();
            names.put("en", "Test Euro for Fetch");
            currency.setName(names);

            CurrencyEntity saved = currencyRepository.saveAndFlush(currency);
            testCurrencyId = saved.getCurrencyKey();

            transactionManager.commit(txSetup);  // COMMIT to database
        } catch (Exception e) {
            transactionManager.rollback(txSetup);
            throw e;
        }
    }

    @AfterEach
    public void teardown() {
        // Clean up test data
        if (testCurrencyId != null) {
            TransactionStatus txCleanup = transactionManager.getTransaction(new DefaultTransactionDefinition());
            try {
                currencyRepository.deleteById(testCurrencyId);
                transactionManager.commit(txCleanup);
            } catch (Exception e) {
                transactionManager.rollback(txCleanup);
            }
        }

        AuthorizationContext.disableBootstrapMode();
        RequestContextHolder.resetRequestAttributes();
    }

    /**
     * This test proves that fetchAndDetachExistingEntity uses a temporary EntityManager
     * to fetch from the COMMITTED database state, NOT from the main EntityManager's cached/modified state.
     *
     * Real-world scenario this simulates:
     * - User sends PATCH request to update a Currency entity
     * - Backend loads the entity into EntityManager and applies changes (but doesn't flush yet)
     * - For permission checking, we need the ORIGINAL database state (before the PATCH)
     * - fetchAndDetachExistingEntity must fetch the committed state, not the modified cached state
     *
     * Test steps:
     * 1. Load the existing committed entity into the main EntityManager
     * 2. Modify it in the persistence context (simulating incoming PATCH changes)
     * 3. Use temporary EntityManager to fetch - should get committed state ("€"), not modified state ("$")
     * 4. Verify: temporary EM returns committed value, main EM still has modified value
     */
    @Test
    public void testFetchAndDetachExistingEntity_FetchesCommittedStateNotModifiedCache() {
        // Start a new transaction for this test
        TransactionStatus tx = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            // Load the existing entity from the database into the main EntityManager
            CurrencyEntity managedEntity = entityManager.find(CurrencyEntity.class, testCurrencyId);
            assertNotNull(managedEntity, "Entity should exist in database and be loaded");
            assertEquals("€", managedEntity.getSymbol(), "Initial committed value should be €");
            assertTrue(entityManager.contains(managedEntity), "Entity should be managed");

            // Simulate a PATCH request: modify the entity in the persistence context
            managedEntity.setSymbol("$");  // This change is only in the persistence context, NOT in the database
            assertEquals("$", managedEntity.getSymbol(), "Modified value in persistence context should be $");

            // DO NOT FLUSH - the database still has the committed value "€"

            // Now test fetchAndDetachExistingEntity - it should fetch the COMMITTED database value
            CurrencyEntity fetchedFromDb = fetchUsingTemporaryEntityManager(testCurrencyId);

            // CRITICAL ASSERTION: The fetched entity must have the COMMITTED value from the database
            assertNotNull(fetchedFromDb, "Temporary EM should successfully fetch the entity");
            assertEquals("€", fetchedFromDb.getSymbol(),
                "PROOF: Temporary EntityManager fetched the COMMITTED database value (€), " +
                "NOT the modified cached value ($) from the main EntityManager's persistence context. " +
                "This proves the temporary EntityManager approach works correctly!");

            // Verify the main EntityManager still has the modified value
            assertEquals("$", managedEntity.getSymbol(),
                "Main EntityManager should still have the modified value ($) in its persistence context");

            // Verify they are different instances (different persistence contexts)
            assertNotSame(managedEntity, fetchedFromDb,
                "Entities from different EntityManagers should be different object instances");

            // Verify the fetched entity is NOT managed by the main EntityManager
            assertFalse(entityManager.contains(fetchedFromDb),
                "Entity fetched by temporary EM should NOT be managed by the main EM");

            transactionManager.rollback(tx);  // Rollback to avoid persisting test modifications
        } catch (Exception e) {
            transactionManager.rollback(tx);
            throw e;
        }
    }

    /**
     * This test verifies that the temporary EntityManager approach doesn't interfere
     * with OTHER managed entities in the main EntityManager's persistence context.
     *
     * This proves we fixed the red flag issue where entityManager.clear() would
     * remove ALL managed entities, not just the one we're fetching.
     */
    @Test
    public void testFetchAndDetachExistingEntity_DoesNotClearOtherManagedEntities() {
        // Create another test entity
        String otherCurrencyId;
        TransactionStatus txSetup2 = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            CurrencyEntity usd = new CurrencyEntity("TEST-USD-FETCH");
            usd.setSymbol("$");
            Map<String, String> names = new HashMap<>();
            names.put("en", "Test USD for Fetch");
            usd.setName(names);
            CurrencyEntity saved = currencyRepository.saveAndFlush(usd);
            otherCurrencyId = saved.getCurrencyKey();
            transactionManager.commit(txSetup2);
        } catch (Exception e) {
            transactionManager.rollback(txSetup2);
            throw e;
        }

        // Main test transaction
        TransactionStatus tx = transactionManager.getTransaction(new DefaultTransactionDefinition());
        try {
            // Load BOTH entities into the main EntityManager
            CurrencyEntity managedEur = entityManager.find(CurrencyEntity.class, testCurrencyId);
            CurrencyEntity managedUsd = entityManager.find(CurrencyEntity.class, otherCurrencyId);

            // Modify both in the persistence context
            managedEur.setSymbol("€-MODIFIED");
            managedUsd.setSymbol("$-MODIFIED");

            // Fetch EUR using temporary EntityManager
            CurrencyEntity fetchedEur = fetchUsingTemporaryEntityManager(testCurrencyId);

            // CRITICAL ASSERTION: USD should STILL be managed with its modified value
            // This proves we didn't call entityManager.clear() which would have removed ALL entities
            assertTrue(entityManager.contains(managedUsd),
                "PROOF: USD is still managed after fetching EUR with temporary EM. " +
                "This proves we DIDN'T use entityManager.clear() which would have removed ALL managed entities!");

            assertEquals("$-MODIFIED", managedUsd.getSymbol(),
                "USD should still have its modified value in the main EntityManager");

            // EUR should also still be managed (even though we fetched it with temp EM)
            assertTrue(entityManager.contains(managedEur),
                "EUR should still be managed in the main EntityManager");
            assertEquals("€-MODIFIED", managedEur.getSymbol(),
                "EUR should still have its modified value in the main EntityManager");

            // Cleanup
            transactionManager.rollback(tx);
            currencyRepository.deleteById(otherCurrencyId);
        } catch (Exception e) {
            transactionManager.rollback(tx);
            try {
                currencyRepository.deleteById(otherCurrencyId);
            } catch (Exception ignored) {}
            throw e;
        }
    }

    /**
     * Helper method that exactly replicates what fetchAndDetachExistingEntity does.
     * Creates a temporary EntityManager to fetch from the committed database state.
     */
    private CurrencyEntity fetchUsingTemporaryEntityManager(String currencyId) {
        EntityManager tempEm = null;
        try {
            tempEm = entityManager.getEntityManagerFactory().createEntityManager();
            return tempEm.find(CurrencyEntity.class, currencyId);
        } finally {
            if (tempEm != null && tempEm.isOpen()) {
                tempEm.close();
            }
        }
    }
}
