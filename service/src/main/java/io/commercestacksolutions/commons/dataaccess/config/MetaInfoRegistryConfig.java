package io.commercestacksolutions.commons.dataaccess.config;

import io.commercestacksolutions.commons.dataaccess.meta.EntityMetaInfoRegistry;
import io.commercestacksolutions.commons.dataaccess.meta.MetaDynamicEnum;
import io.commercestacksolutions.commons.dataaccess.meta.MetaInfoBuilder;
import io.commercestacksolutions.commons.web.rest.MetaInfo;
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
                    List<String> beanNames = new ArrayList<>(applicationContext.getBeansOfType(beanType).keySet());
                    enumValues.put(field.getName(), beanNames);
                }
            }
            clazz = clazz.getSuperclass();
        }

        if (!enumValues.isEmpty()) {
            metaInfo.setEnumValues(enumValues);
        }
        return metaInfo;
    }
}
