package io.commercestacksolutions.commons.dataaccess.idgenerator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Static provider that makes the configured {@link IdGenerator} available to JPA entities
 * (which are not Spring beans and cannot use dependency injection directly).
 * Initialized by Spring on application startup.
 */
@Component
public class IdGeneratorProvider {

    private static IdGenerator instance;

    @Autowired
    public void setIdGenerator(IdGenerator idGenerator) {
        IdGeneratorProvider.instance = idGenerator;
    }

    /**
     * Generate a new entity ID using the configured {@link IdGenerator}.
     * Falls back to {@link UUID#randomUUID()} if Spring has not yet initialised the provider.
     */
    public static String generate() {
        if (instance != null) {
            return instance.generateId();
        }
        return UUID.randomUUID().toString();
    }
}
