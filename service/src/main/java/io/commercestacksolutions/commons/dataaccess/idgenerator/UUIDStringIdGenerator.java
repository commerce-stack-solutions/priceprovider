package io.commercestacksolutions.commons.dataaccess.idgenerator;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Default {@link IdGenerator} that generates random UUID strings.
 * Override this bean with a {@code @Primary} bean to use a different strategy.
 */
@Component
@Primary
public class UUIDStringIdGenerator implements IdGenerator {

    @Override
    public String generateId() {
        return UUID.randomUUID().toString();
    }
}
