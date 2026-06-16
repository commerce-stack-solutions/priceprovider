package io.commercestacksolutions.commons.dataaccess.config;

import io.commercestacksolutions.commons.dataaccess.meta.EntityMetaInfoRegistry;
import io.commercestacksolutions.commons.dataaccess.meta.EnumTypeValueDefinition;
import io.commercestacksolutions.commons.dataaccess.meta.EnumTypeValueRegistry;
import io.commercestacksolutions.commons.dataaccess.meta.MetaDynamicEnum;
import io.commercestacksolutions.commons.dataaccess.meta.MetaInfoBuilder;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppPermissionEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.approle.entity.AppRoleEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.country.entity.CountryEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.organization.entity.OrganizationEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import jakarta.annotation.PostConstruct;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Populates the {@link EntityMetaInfoRegistry} at application startup.
 *
 * <p>All entity MetaInfo objects are built once via {@link MetaInfoBuilder}
 * during server initialization and stored in the registry for reuse on
 * every subsequent request.</p>
 *
 * <p>Fields annotated with {@link MetaDynamicEnum} are enriched with the Spring bean
 * names of all registered beans of the declared {@code beanType}, making the allowed
 * values available via the {@code $meta} REST response.</p>
 */
@Component
public class MetaInfoRegistryConfig {

    private final EntityMetaInfoRegistry entityMetaInfoRegistry;
    private final ApplicationContext applicationContext;

    public MetaInfoRegistryConfig(EntityMetaInfoRegistry entityMetaInfoRegistry, ApplicationContext applicationContext) {
        this.entityMetaInfoRegistry = entityMetaInfoRegistry;
        this.applicationContext = applicationContext;
    }

    @PostConstruct
    public void registerEntityMetaInfos() {
        entityMetaInfoRegistry.register(AppPermissionEntity.class, build(AppPermissionEntity.class));
        entityMetaInfoRegistry.register(AppRoleEntity.class,       build(AppRoleEntity.class));
        entityMetaInfoRegistry.register(GroupEntity.class,        build(GroupEntity.class));
        entityMetaInfoRegistry.register(OrganizationEntity.class, build(OrganizationEntity.class));
        entityMetaInfoRegistry.register(UnitEntity.class,         build(UnitEntity.class));
        entityMetaInfoRegistry.register(ChannelEntity.class,      build(ChannelEntity.class));
        entityMetaInfoRegistry.register(CurrencyEntity.class,     build(CurrencyEntity.class));
        entityMetaInfoRegistry.register(LanguageEntity.class,     build(LanguageEntity.class));
        entityMetaInfoRegistry.register(CountryEntity.class,      build(CountryEntity.class));
        entityMetaInfoRegistry.register(TaxClassEntity.class,     build(TaxClassEntity.class));
        entityMetaInfoRegistry.register(PriceRowEntity.class,     build(PriceRowEntity.class));
    }

    /**
     * Builds MetaInfo for the given entity class, enriching it with dynamic enum values
     * from fields annotated with {@link MetaDynamicEnum}.
     */
    private MetaInfo build(Class<?> entityClass) {
        MetaInfo metaInfo = MetaInfoBuilder.build(entityClass);

        Map<String, List<String>> enumValues = metaInfo.getEnumValues() != null
                ? new HashMap<>(metaInfo.getEnumValues())
                : new HashMap<>();

        Class<?> clazz = entityClass;
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                MetaDynamicEnum annotation = field.getAnnotation(MetaDynamicEnum.class);
                if (annotation != null) {
                    Class<?> beanType = annotation.beanType();
                    List<String> keys = resolveDynamicEnumValues(beanType);
                    enumValues.put(field.getName(), keys);
                }
            }
            clazz = clazz.getSuperclass();
        }

        if (!enumValues.isEmpty()) {
            metaInfo.setEnumValues(enumValues);
        }
        return metaInfo;
    }

    private List<String> resolveDynamicEnumValues(Class<?> beanType) {
        // Try to find a registry for this bean type first
        Map<String, EnumTypeValueRegistry> registries = applicationContext.getBeansOfType(EnumTypeValueRegistry.class);
        for (EnumTypeValueRegistry<?, ?> registry : registries.values()) {
            // Check if the registry manages this bean type (definition type)
            // This is a bit tricky with generics at runtime, so we look at the definitions
            List<?> definitions = registry.getDefinitions();
            if (!definitions.isEmpty() && beanType.isAssignableFrom(definitions.get(0).getClass())) {
                return registry.getCodes().stream().map(code -> code.toUpperCase()).toList();
            }
        }

        // Fallback: collect from all beans of the given type
        Map<String, ?> beans = applicationContext.getBeansOfType(beanType);
        return beans.values().stream()
                .map(bean -> {
                    if (bean instanceof EnumTypeValueDefinition<?> definition) {
                        Object value = definition.getValue();
                        if (value != null) {
                            // Use reflection to call code() if it's a record or has a code() method
                            try {
                                return (String) value.getClass().getMethod("code").invoke(value);
                            } catch (Exception e) {
                                // Fallback to toString()
                                return value.toString();
                            }
                        }
                    }
                    // If not an EnumTypeValueDefinition, use the bean name (original behavior)
                    // Find the bean name for this bean instance
                    return beans.entrySet().stream()
                            .filter(entry -> entry.getValue() == bean)
                            .map(Map.Entry::getKey)
                            .findFirst()
                            .orElse(bean.toString());
                })
                .map(String::toUpperCase)
                .distinct()
                .toList();
    }
}
