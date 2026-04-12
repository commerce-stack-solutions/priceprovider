package io.commercestacksolutions.commons.dataaccess;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as the natural reference key for its entity.
 * When {@link io.commercestacksolutions.commons.query.SpecificationBuilder SpecificationBuilder}
 * builds a {@code hasAny} or {@code hasAll} predicate for a collection of this entity type,
 * it uses the annotated field for matching instead of the technical {@code @Id} field.
 *
 * <p>Use this when the business/query values supplied by callers correspond to a
 * human-readable identifier (e.g. {@code path}) rather than the technical primary key.</p>
 *
 * <p>This annotation is intentionally placed in the {@code commons.dataaccess} package
 * because it is part of the data-access model metadata, not specific to query building.</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReferenceKey {
}
