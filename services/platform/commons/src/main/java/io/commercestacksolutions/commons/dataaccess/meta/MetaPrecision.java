package io.commercestacksolutions.commons.dataaccess.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a numeric field with its decimal precision (scale) for metadata exposure.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MetaPrecision {
    int value() default 2;
}
