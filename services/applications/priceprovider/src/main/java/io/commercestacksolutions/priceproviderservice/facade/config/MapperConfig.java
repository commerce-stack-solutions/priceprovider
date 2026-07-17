package io.commercestacksolutions.priceproviderservice.facade.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.commercestacksolutions.commons.mapper.GenericPatchMapper;
import io.commercestacksolutions.commons.mapper.PatchMapper;
import io.commercestacksolutions.priceproviderservice.facade.language.restentity.LanguageRestEntity;
import io.commercestacksolutions.priceproviderservice.facade.pricerow.restentity.PriceRowRestEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class MapperConfig {

    @Bean
    public PatchMapper<PriceRowRestEntity> priceRowRestEntityPatchMapper(ObjectMapper objectMapper) {
        return new GenericPatchMapper<>(objectMapper, PriceRowRestEntity.class);
    }

    @Bean
    public PatchMapper<LanguageRestEntity> languageRestEntityPatchMapper(ObjectMapper objectMapper) {
        return new GenericPatchMapper<>(objectMapper, LanguageRestEntity.class);
    }

    @Bean
    public Module bigDecimalPlainModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(BigDecimal.class, new BigDecimalPlainSerializer());
        return module;
    }

    @Bean
    public JavaTimeModule javaTimeModule() {
        return new JavaTimeModule();
    }
}
