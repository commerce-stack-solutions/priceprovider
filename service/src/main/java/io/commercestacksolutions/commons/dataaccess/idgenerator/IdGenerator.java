package io.commercestacksolutions.commons.dataaccess.idgenerator;

/**
 * Strategy interface for generating entity IDs.
 * Provide a primary Spring bean implementing this interface to customize the generation strategy.
 */
public interface IdGenerator {
    String generateId();
}
