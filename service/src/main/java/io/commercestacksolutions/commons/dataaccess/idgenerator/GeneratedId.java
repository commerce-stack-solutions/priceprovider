package io.commercestacksolutions.commons.dataaccess.idgenerator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as an auto-generated technical identifier.
 * Use this alongside {@code jakarta.persistence.@Id} on fields whose value is
 * assigned automatically (e.g. via {@code @PrePersist} with an {@link IdGenerator})
 * rather than supplied by the caller.
 *
 * <p>{@link io.commercestacksolutions.commons.dataaccess.meta.MetaInfoBuilder MetaInfoBuilder}
 * treats fields annotated with both {@code @Id} and {@code @GeneratedId} the same as
 * fields annotated with {@code @Id @GeneratedValue}: they are listed as identity fields
 * but are <strong>not</strong> included in the mandatory fields list.</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface GeneratedId {
}
