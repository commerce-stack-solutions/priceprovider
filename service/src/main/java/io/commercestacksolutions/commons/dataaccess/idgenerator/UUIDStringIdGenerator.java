package io.commercestacksolutions.commons.dataaccess.idgenerator;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Default {@link IdGenerator} that generates random UUID strings.
 *
 * <p>This bean is registered as {@code @Primary} so it serves as the global default when no
 * other {@code @Primary} implementation is present.  To customize the generation strategy:</p>
 * <ul>
 *   <li><b>Replace globally:</b> provide your own {@code @Primary @Component} implementing
 *       {@link IdGenerator}.  This will be picked up application-wide without touching any
 *       entity code (open-closed principle).</li>
 *   <li><b>Override per entity:</b> annotate your implementation with
 *       {@link ForEntity @ForEntity(MyEntity.class)} and ensure the entity's
 *       {@code @PrePersist} calls {@link IdGeneratorProvider#generate(Class)}.
 *       The entity-specific generator is used only for that entity; all others continue
 *       to use the global default.</li>
 * </ul>
 */
@Component
@Primary
public class UUIDStringIdGenerator implements IdGenerator {

    @Override
    public String generateId() {
        return UUID.randomUUID().toString();
    }
}
