package io.commercestacksolutions.commons.dataaccess;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as the human-readable alternative key for its entity, used in JSON references
 * and queries instead of the generated technical {@code @Id} field.
 *
 * <h3>Effects</h3>
 * <ul>
 *   <li><strong>SpecificationBuilder</strong>: When building a {@code hasAny} or {@code hasAll}
 *       predicate for a collection of this entity type, the annotated field is used for matching
 *       instead of the technical {@code @Id} field.  This allows callers to filter by
 *       human-readable identifiers (e.g. {@code path}) in Lucene-style query strings.</li>
 *   <li><strong>MetaInfoBuilder / {@code $meta}</strong>: The annotated field name is exposed
 *       as {@code referenceKeyFields} in the {@code $meta} section of REST responses.  Clients
 *       can use this to discover which field to use when constructing references.  If no field
 *       carries this annotation, {@code $meta.referenceKeyFields} falls back to the entity's
 *       {@code @Id} field(s).</li>
 * </ul>
 *
 * <p>This annotation is placed in the {@code commons.dataaccess} package because it is part of
 * the data-access model metadata, not specific to query building.</p>
 *
 * <h3>Example</h3>
 * <pre>{@code
 * public class GroupEntity {
 *     @Id @GeneratedId
 *     private String id;          // technical UUID – never used in JSON refs
 *
 *     @ReferenceKey
 *     private String path;        // human-readable key – used in JSON refs and queries
 * }
 * }</pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ReferenceKey {
}
