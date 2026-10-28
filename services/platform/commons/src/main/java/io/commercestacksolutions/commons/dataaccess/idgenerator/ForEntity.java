package io.commercestacksolutions.commons.dataaccess.idgenerator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an {@link IdGenerator} Spring bean as the preferred generator for one or more
 * specific entity classes.
 *
 * <p>When {@link IdGeneratorProvider#generate(Class)} is called with an entity class,
 * the provider first checks whether any registered bean carries {@code @ForEntity} with
 * that class.  If a match is found it takes precedence over the global {@code @Primary}
 * bean.  If multiple beans declare the same entity class, the one also annotated
 * {@code @Primary} wins; otherwise the first one encountered wins.</p>
 *
 * <h3>Example – custom generator for a single entity</h3>
 * <pre>{@code
 * @Component
 * @ForEntity(GroupEntity.class)
 * public class PartitionedGroupIdGenerator implements IdGenerator {
 *     @Override
 *     public String generateId() {
 *         return "GRP-" + UUID.randomUUID();
 *     }
 * }
 * }</pre>
 *
 * <h3>Example – custom generator as global default AND for specific entities</h3>
 * <pre>{@code
 * @Primary
 * @Component
 * @ForEntity({ GroupEntity.class, PriceRowEntity.class })
 * public class SpannerIdGenerator implements IdGenerator { ... }
 * }</pre>
 *
 * <p>Entities that want to participate in entity-specific selection should call
 * {@link IdGeneratorProvider#generate(Class)} from their {@code @PrePersist} method:</p>
 * <pre>{@code
 * @PrePersist
 * protected void prePersist() {
 *     if (this.id == null) {
 *         this.id = IdGeneratorProvider.generate(GroupEntity.class);
 *     }
 * }
 * }</pre>
 *
 * @see IdGenerator
 * @see IdGeneratorProvider
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ForEntity {
    /**
     * The entity class(es) for which this generator should be used.
     */
    Class<?>[] value();
}
