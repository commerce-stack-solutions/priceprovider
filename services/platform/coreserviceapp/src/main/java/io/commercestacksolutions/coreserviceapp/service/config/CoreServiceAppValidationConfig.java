package io.commercestacksolutions.coreserviceapp.service.config;

import io.commercestacksolutions.commons.dataaccess.meta.EntityMetaInfoRegistry;
import io.commercestacksolutions.commons.service.entity.validation.ValidationRule;
import io.commercestacksolutions.commons.service.entity.validation.rules.RequireMandatoryFieldsRule;
import io.commercestacksolutions.coreserviceapp.dataaccess.approle.entity.AppRoleEntity;
import io.commercestacksolutions.coreserviceapp.dataaccess.approle.entity.CommonAppPermission;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreServiceAppValidationConfig {

    @Bean
    public ValidationRule<CommonAppPermission> appPermissionRequireMandatoryFieldsRule(EntityMetaInfoRegistry registry) {
        return new RequireMandatoryFieldsRule<>(CommonAppPermission.class, registry);
    }

    @Bean
    public ValidationRule<AppRoleEntity> appRoleRequireMandatoryFieldsRule(EntityMetaInfoRegistry registry) {
        return new RequireMandatoryFieldsRule<>(AppRoleEntity.class, registry);
    }
}
