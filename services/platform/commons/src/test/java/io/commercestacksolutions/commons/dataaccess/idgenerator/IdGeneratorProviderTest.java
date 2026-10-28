package io.commercestacksolutions.commons.dataaccess.idgenerator;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link IdGeneratorProvider}.
 *
 * <p>Tests the three-tier generator selection strategy:
 * <ol>
 *   <li>Entity-specific generator (registered via {@link ForEntity @ForEntity})</li>
 *   <li>Global default (the {@code @Primary} bean)</li>
 *   <li>Hard-coded {@link java.util.UUID#randomUUID()} fallback when Spring is not available</li>
 * </ol>
 */
public class IdGeneratorProviderTest {

    /** Captures generated IDs in unit tests, avoiding UUID parsing. */
    static class FixedIdGenerator implements IdGenerator {
        private final String fixedId;

        FixedIdGenerator(String fixedId) {
            this.fixedId = fixedId;
        }

        @Override
        public String generateId() {
            return fixedId;
        }
    }

    static class DomainA {}
    static class DomainB {}

    @ForEntity(DomainA.class)
    static class DomainAGenerator extends FixedIdGenerator {
        DomainAGenerator() {
            super("DOMAIN-A-ID");
        }
    }

    @ForEntity({DomainA.class, DomainB.class})
    static class MultiEntityGenerator extends FixedIdGenerator {
        MultiEntityGenerator() {
            super("MULTI-ENTITY-ID");
        }
    }

    private IdGeneratorProvider provider;

    @BeforeEach
    void setUp() throws Exception {
        provider = new IdGeneratorProvider();
        // Reset static state between tests
        resetStaticFields();
    }

    @AfterEach
    void tearDown() throws Exception {
        resetStaticFields();
    }

    private void resetStaticFields() throws Exception {
        Field defaultField = IdGeneratorProvider.class.getDeclaredField("defaultGenerator");
        defaultField.setAccessible(true);
        defaultField.set(null, null);

        Field entityGeneratorsField = IdGeneratorProvider.class.getDeclaredField("entityGenerators");
        entityGeneratorsField.setAccessible(true);
        ((Map<?, ?>) entityGeneratorsField.get(null)).clear();
    }

    // ---------- Global default ----------

    @Test
    void generate_withGlobalDefault_usesDefaultGenerator() {
        FixedIdGenerator global = new FixedIdGenerator("GLOBAL-ID");
        provider.setDefaultGenerator(global);

        assertEquals("GLOBAL-ID", IdGeneratorProvider.generate());
    }

    @Test
    void generate_withNullEntityClass_usesGlobalDefault() {
        FixedIdGenerator global = new FixedIdGenerator("GLOBAL-ID");
        provider.setDefaultGenerator(global);

        assertEquals("GLOBAL-ID", IdGeneratorProvider.generate(null));
    }

    @Test
    void generate_withUnknownEntityClass_fallsBackToGlobalDefault() {
        FixedIdGenerator global = new FixedIdGenerator("GLOBAL-ID");
        provider.setDefaultGenerator(global);

        assertEquals("GLOBAL-ID", IdGeneratorProvider.generate(String.class));
    }

    // ---------- Fallback when Spring is not initialised ----------

    @Test
    void generate_withNoDefaultGenerator_returnsUUIDFallback() {
        // No generator set – simulate missing Spring context
        String id = IdGeneratorProvider.generate();
        assertNotNull(id, "Fallback must return a non-null ID");
        assertFalse(id.isBlank(), "Fallback must return a non-blank ID");
        // UUID format: 8-4-4-4-12
        assertTrue(id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),
                "Fallback must produce a valid UUID string, got: " + id);
    }

    // ---------- Per-entity generator via @ForEntity ----------

    @Test
    void generate_withForEntityGenerator_usesEntitySpecificGenerator() {
        FixedIdGenerator global = new FixedIdGenerator("GLOBAL-ID");
        provider.setDefaultGenerator(global);
        provider.setAllGenerators(List.of(new DomainAGenerator()));

        assertEquals("DOMAIN-A-ID", IdGeneratorProvider.generate(DomainA.class),
                "Entity-specific generator must take priority over global default");
    }

    @Test
    void generate_withForEntityGenerator_doesNotAffectOtherEntities() {
        FixedIdGenerator global = new FixedIdGenerator("GLOBAL-ID");
        provider.setDefaultGenerator(global);
        provider.setAllGenerators(List.of(new DomainAGenerator()));

        assertEquals("GLOBAL-ID", IdGeneratorProvider.generate(DomainB.class),
                "Entity-specific generator for DomainA must not affect DomainB");
    }

    @Test
    void generate_withMultiEntityForEntity_registersForAllDeclaredEntities() {
        FixedIdGenerator global = new FixedIdGenerator("GLOBAL-ID");
        provider.setDefaultGenerator(global);
        provider.setAllGenerators(List.of(new MultiEntityGenerator()));

        assertEquals("MULTI-ENTITY-ID", IdGeneratorProvider.generate(DomainA.class));
        assertEquals("MULTI-ENTITY-ID", IdGeneratorProvider.generate(DomainB.class));
    }

    @Test
    void setAllGenerators_withNullList_doesNotThrow() {
        assertDoesNotThrow(() -> provider.setAllGenerators(null),
                "setAllGenerators must handle null gracefully");
    }

    @Test
    void setAllGenerators_withGeneratorsWithoutForEntity_doesNotRegisterEntityMapping() {
        FixedIdGenerator global = new FixedIdGenerator("GLOBAL-ID");
        provider.setDefaultGenerator(global);
        // Plain generator without @ForEntity annotation
        provider.setAllGenerators(List.of(new FixedIdGenerator("PLAIN")));

        // Plain generator is not registered for any entity – falls back to global
        assertEquals("GLOBAL-ID", IdGeneratorProvider.generate(DomainA.class));
    }

    // ---------- UUIDStringIdGenerator ----------

    @Test
    void uuidStringIdGenerator_producesValidUUID() {
        UUIDStringIdGenerator gen = new UUIDStringIdGenerator();
        String id = gen.generateId();
        assertNotNull(id);
        assertTrue(id.matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"),
                "UUIDStringIdGenerator must produce a valid UUID string, got: " + id);
    }

    @Test
    void uuidStringIdGenerator_producesUniqueValues() {
        UUIDStringIdGenerator gen = new UUIDStringIdGenerator();
        String id1 = gen.generateId();
        String id2 = gen.generateId();
        assertNotEquals(id1, id2, "UUIDStringIdGenerator must produce unique IDs");
    }
}
