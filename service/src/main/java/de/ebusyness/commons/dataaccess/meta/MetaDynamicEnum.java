package de.ebusyness.commons.dataaccess.meta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code String} entity field whose allowed values are the Spring bean names
 * of all registered beans implementing {@link #beanType()}.
 *
 * <p>At application startup, {@code MetaInfoRegistryConfig} queries the Spring
 * {@code ApplicationContext} for all beans of {@code beanType} and adds their names
 * to the {@code enumValues} map in the entity's {@link de.ebusyness.commons.web.rest.MetaInfo}.
 * This allows the frontend to populate a dynamic dropdown from the API's {@code $meta} response.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * @MetaDynamicEnum(beanType = PriceRepresentationMode.class)
 * @MetaMandatoryField
 * private String priceRepresentationMode;
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MetaDynamicEnum {

    /**
     * The Spring bean type whose registered bean names are the allowed values for this field.
     */
    Class<?> beanType();
}
