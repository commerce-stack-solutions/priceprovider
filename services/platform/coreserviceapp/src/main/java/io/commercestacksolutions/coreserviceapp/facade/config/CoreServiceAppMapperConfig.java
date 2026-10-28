package io.commercestacksolutions.coreserviceapp.facade.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.commercestacksolutions.commons.mapper.GenericPatchMapper;
import io.commercestacksolutions.commons.mapper.PatchMapper;
import io.commercestacksolutions.coreserviceapp.facade.approle.restentity.AppPermissionRestEntity;
import io.commercestacksolutions.coreserviceapp.facade.approle.restentity.AppRoleRestEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreServiceAppMapperConfig {

    @Bean
    public PatchMapper<AppPermissionRestEntity> appPermissionRestEntityPatchMapper(ObjectMapper objectMapper) {
        return new GenericPatchMapper<>(objectMapper, AppPermissionRestEntity.class);
    }

    @Bean
    public PatchMapper<AppRoleRestEntity> appRoleRestEntityPatchMapper(ObjectMapper objectMapper) {
        return new GenericPatchMapper<>(objectMapper, AppRoleRestEntity.class);
    }
}
