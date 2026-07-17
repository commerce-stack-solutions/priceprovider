package io.commercestacksolutions.priceproviderservice.service.config;

import io.commercestacksolutions.commons.dataaccess.meta.EntityMetaInfoRegistry;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.service.entity.validation.rules.RequireMandatoryFieldsRule;
import io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity.PriceRowEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RequireMandatoryFieldsValidationConfig {

    @Bean
    public ValidationRule<PriceRowEntity> priceRowRequireMandatoryFieldsRule(EntityMetaInfoRegistry registry) {
        return new RequireMandatoryFieldsRule<>(PriceRowEntity.class, registry);
    }
}
