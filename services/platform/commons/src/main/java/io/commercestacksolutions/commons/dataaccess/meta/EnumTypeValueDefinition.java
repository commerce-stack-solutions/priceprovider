package io.commercestacksolutions.commons.dataaccess.meta;

/**
 * Interface for definitions of dynamic enum values.
 *
 * @param <T> the type of the value object (e.g. PriceType, OrganizationType)
 */
public interface EnumTypeValueDefinition<T> {
    /**
     * Returns the value object defined by this bean.
     */
    T getValue();
}
