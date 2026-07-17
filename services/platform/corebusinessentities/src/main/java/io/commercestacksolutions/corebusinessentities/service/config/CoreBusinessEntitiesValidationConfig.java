package io.commercestacksolutions.corebusinessentities.service.config;

import io.commercestacksolutions.commons.dataaccess.meta.EntityMetaInfoRegistry;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.service.entity.validation.rules.RequireMandatoryFieldsRule;
import io.commercestacksolutions.corebusinessentities.dataaccess.channel.entity.ChannelEntity;
import io.commercestacksolutions.corebusinessentities.dataaccess.country.entity.CountryEntity;
import io.commercestacksolutions.corebusinessentities.dataaccess.currency.entity.CurrencyEntity;
import io.commercestacksolutions.corebusinessentities.dataaccess.group.entity.GroupEntity;
import io.commercestacksolutions.corebusinessentities.dataaccess.organization.entity.OrganizationEntity;
import io.commercestacksolutions.corebusinessentities.dataaccess.taxclass.entity.TaxClassEntity;
import io.commercestacksolutions.corebusinessentities.dataaccess.unit.entity.UnitEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreBusinessEntitiesValidationConfig {

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
}
