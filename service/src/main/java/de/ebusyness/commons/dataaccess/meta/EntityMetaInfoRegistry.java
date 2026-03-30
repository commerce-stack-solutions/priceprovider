package de.ebusyness.commons.dataaccess.meta;

import de.ebusyness.commons.web.rest.MetaInfo;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring component that pre-builds and caches MetaInfo instances for all registered
 * entity classes at application startup.
 *
 * <p>Facades inject this registry and retrieve the pre-built MetaInfo instead of
 * calling MetaInfoBuilder on every request, ensuring the reflection-based scan is
 * performed only once.</p>
 */
@Component
public class EntityMetaInfoRegistry {

    private final Map<Class<?>, MetaInfo> registry = new HashMap<>();

    /**
     * Registers the MetaInfo for a given entity class.
     * Called by the MetaInfoRegistryConfig configuration at startup.
     *
     * @param entityClass the entity class
     * @param metaInfo    the pre-built MetaInfo instance
     */
    public void register(Class<?> entityClass, MetaInfo metaInfo) {
        registry.put(entityClass, metaInfo);
    }

    /**
     * Returns the pre-built MetaInfo for the given entity class,
     * or {@code null} if the class has not been registered.
     *
     * @param entityClass the entity class
     * @return the MetaInfo or {@code null}
     */
    public MetaInfo getMetaInfo(Class<?> entityClass) {
        return registry.get(entityClass);
    }
}
