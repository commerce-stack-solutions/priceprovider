package io.commercestacksolutions.commons.idgenerator;

/**
 * Strategy interface for generating entity IDs.
 * Provide a primary Spring bean implementing this interface to customize the generation strategy.
 */
public interface IdGenerator {
    String generateId();
}
