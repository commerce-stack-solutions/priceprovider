package io.commercestacksolutions.commons.dataaccess.idgenerator;

/**
 * Strategy interface for generating entity IDs.
 *
 * <p>Two selection mechanisms are supported:</p>
 * <ol>
 *   <li><b>Global default:</b> annotate your implementation with {@code @Primary} to replace
 *       {@link UUIDStringIdGenerator} application-wide.</li>
 *   <li><b>Per-entity override:</b> annotate your implementation with
 *       {@link ForEntity @ForEntity(MyEntity.class)} to use it only for a specific entity class,
 *       without affecting other entities.  The entity's {@code @PrePersist} method must call
 *       {@link IdGeneratorProvider#generate(Class)} with its own class for this to take effect.</li>
 * </ol>
 *
 * @see ForEntity
 * @see IdGeneratorProvider
 * @see UUIDStringIdGenerator
 */
public interface IdGenerator {
    String generateId();
}
