package io.commercestacksolutions.priceproviderservice.service.config;

import io.commercestacksolutions.commons.dataaccess.meta.EntityMetaInfoRegistry;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.service.entity.validation.rules.RequireMandatoryFieldsRule;
import io.commercestacksolutions.priceproviderservice.dataaccess.channel.entity.ChannelEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.country.entity.CountryEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.language.entity.LanguageEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.organization.entity.OrganizationEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity;
import io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers one {@link RequireMandatoryFieldsRule} bean per entity class.
 *
 * <p>Each bean is typed to its entity (e.g. {@code ValidationRule<GroupEntity>})
 * so that Spring auto-wires the correct instance into the corresponding
 * {@code *ServiceImpl} constructor via {@code List<ValidationRule<T>> validationRules}.</p>
 *
 * <p>The mandatory field list for each entity is driven by the
 * {@link EntityMetaInfoRegistry} (pre-built at startup by
 * {@code MetaInfoRegistryConfig}), which reads {@code @Id} and
 * {@code @MetaMandatoryField} annotations on the entity class hierarchy.</p>
 */
@Configuration
public class RequireMandatoryFieldsValidationConfig {

    @Bean
    public ValidationRule<GroupEntity> groupRequireMandatoryFieldsRule(EntityMetaInfoRegistry registry) {
        return new RequireMandatoryFieldsRule<>(GroupEntity.class, registry);
    }

    @Bean
    public ValidationRule<OrganizationEntity> organizationRequireMandatoryFieldsRule(EntityMetaInfoRegistry registry) {
        return new RequireMandatoryFieldsRule<>(OrganizationEntity.class, registry);
    }

    @Bean
    public ValidationRule<UnitEntity> unitRequireMandatoryFieldsRule(EntityMetaInfoRegistry registry) {
        return new RequireMandatoryFieldsRule<>(UnitEntity.class, registry);
    }

    @Bean
    public ValidationRule<CurrencyEntity> currencyRequireMandatoryFieldsRule(EntityMetaInfoRegistry registry) {
        return new RequireMandatoryFieldsRule<>(CurrencyEntity.class, registry);
    }

    @Bean
    public ValidationRule<LanguageEntity> languageRequireMandatoryFieldsRule(EntityMetaInfoRegistry registry) {
        return new RequireMandatoryFieldsRule<>(LanguageEntity.class, registry);
    }

    @Bean
    public ValidationRule<CountryEntity> countryRequireMandatoryFieldsRule(EntityMetaInfoRegistry registry) {
        return new RequireMandatoryFieldsRule<>(CountryEntity.class, registry);
    }

    @Bean
    public ValidationRule<ChannelEntity> channelRequireMandatoryFieldsRule(EntityMetaInfoRegistry registry) {
        return new RequireMandatoryFieldsRule<>(ChannelEntity.class, registry);
    }

    @Bean
    public ValidationRule<TaxClassEntity> taxClassRequireMandatoryFieldsRule(EntityMetaInfoRegistry registry) {
        return new RequireMandatoryFieldsRule<>(TaxClassEntity.class, registry);
    }

    @Bean
    public ValidationRule<PriceRowEntity> priceRowRequireMandatoryFieldsRule(EntityMetaInfoRegistry registry) {
        return new RequireMandatoryFieldsRule<>(PriceRowEntity.class, registry);
    }
}
