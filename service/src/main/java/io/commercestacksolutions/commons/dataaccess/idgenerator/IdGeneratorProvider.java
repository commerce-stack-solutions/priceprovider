package io.commercestacksolutions.commons.dataaccess.idgenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static provider that makes the configured {@link IdGenerator} available to JPA entities
 * (which are not Spring beans and cannot use dependency injection directly).
 * Initialized by Spring on application startup.
 *
 * <h3>Global default</h3>
 * The Spring bean marked {@code @Primary} (default: {@link UUIDStringIdGenerator}) is used
 * when {@link #generate()} is called or when no entity-specific generator is registered.
 *
 * <h3>Per-entity generator</h3>
 * Any {@link IdGenerator} bean annotated with {@link ForEntity @ForEntity} is registered as
 * the preferred generator for the specified entity class(es).  When
 * {@link #generate(Class)} is called with a matching entity class, the entity-specific
 * generator takes precedence over the global default.
 */
@Component
public class IdGeneratorProvider {

    private static IdGenerator defaultGenerator;
    private static final Map<Class<?>, IdGenerator> entityGenerators = new ConcurrentHashMap<>();

    /**
     * Injects the global default generator (the {@code @Primary} bean).
     */
    @Autowired
    public void setDefaultGenerator(IdGenerator defaultGenerator) {
        IdGeneratorProvider.defaultGenerator = defaultGenerator;
    }

    /**
     * Scans all {@link IdGenerator} beans for {@link ForEntity @ForEntity} annotations and
     * registers entity-specific mappings.  Beans without the annotation are ignored here
     * (they may still serve as the global default via {@code @Primary}).
     */
    @Autowired(required = false)
    public void setAllGenerators(List<IdGenerator> allGenerators) {
        if (allGenerators == null) {
            return;
        }
        for (IdGenerator generator : allGenerators) {
            ForEntity forEntity = AnnotationUtils.findAnnotation(generator.getClass(), ForEntity.class);
            if (forEntity != null) {
                for (Class<?> entityClass : forEntity.value()) {
                    entityGenerators.put(entityClass, generator);
                }
            }
        }
    }

    /**
     * Generate a new entity ID using the global default {@link IdGenerator}.
     * Falls back to {@link UUID#randomUUID()} if Spring has not yet initialised the provider.
     */
    public static String generate() {
        return generate(null);
    }

    /**
     * Generate a new entity ID, preferring the entity-specific generator registered via
     * {@link ForEntity @ForEntity} for the given {@code entityClass}.
     * Falls back to the global {@code @Primary} generator, then to {@link UUID#randomUUID()}.
     *
     * @param entityClass the entity class requesting an ID, or {@code null} for the global default
     */
    public static String generate(Class<?> entityClass) {
        if (entityClass != null) {
            IdGenerator entityGen = entityGenerators.get(entityClass);
            if (entityGen != null) {
                return entityGen.generateId();
            }
        }
        if (defaultGenerator != null) {
            return defaultGenerator.generateId();
        }
        return UUID.randomUUID().toString();
    }
}
