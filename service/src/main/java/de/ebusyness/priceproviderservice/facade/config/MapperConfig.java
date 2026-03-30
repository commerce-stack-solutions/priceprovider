package de.ebusyness.priceproviderservice.facade.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import de.ebusyness.commons.mapper.PatchMapper;
import de.ebusyness.commons.mapper.GenericPatchMapper;
import de.ebusyness.priceproviderservice.facade.approle.restentity.AppPermissionRestEntity;
import de.ebusyness.priceproviderservice.facade.approle.restentity.AppRoleRestEntity;
import de.ebusyness.priceproviderservice.facade.channel.restentity.ChannelRestEntity;
import de.ebusyness.priceproviderservice.facade.country.restentity.CountryRestEntity;
import de.ebusyness.priceproviderservice.facade.currency.restentity.CurrencyRestEntity;
import de.ebusyness.priceproviderservice.facade.group.restentity.GroupRestEntity;
import de.ebusyness.priceproviderservice.facade.language.restentity.LanguageRestEntity;
import de.ebusyness.priceproviderservice.facade.organization.restentity.OrganizationRestEntity;
import de.ebusyness.priceproviderservice.facade.pricerow.restentity.PriceRowRestEntity;
import de.ebusyness.priceproviderservice.facade.taxclass.restentity.TaxClassRestEntity;
import de.ebusyness.priceproviderservice.facade.unit.restentity.UnitRestEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;

@Configuration
public class MapperConfig {

    @Bean
    public PatchMapper<UnitRestEntity> unitRestEntityPatchMapper(ObjectMapper objectMapper) {
        return new GenericPatchMapper<>(objectMapper, UnitRestEntity.class);
    }

    @Bean
    public PatchMapper<PriceRowRestEntity> priceRowRestEntityPatchMapper(ObjectMapper objectMapper) {
        return new GenericPatchMapper<>(objectMapper, PriceRowRestEntity.class);
    }

    @Bean
    public PatchMapper<TaxClassRestEntity> taxClassRestEntityPatchMapper(ObjectMapper objectMapper) {
        return new GenericPatchMapper<>(objectMapper, TaxClassRestEntity.class);
    }

    @Bean
    public PatchMapper<LanguageRestEntity> languageRestEntityPatchMapper(ObjectMapper objectMapper) {
        return new GenericPatchMapper<>(objectMapper, LanguageRestEntity.class);
    }

    @Bean
    public PatchMapper<CurrencyRestEntity> currencyRestEntityPatchMapper(ObjectMapper objectMapper) {
        return new GenericPatchMapper<>(objectMapper, CurrencyRestEntity.class);
    }

    @Bean
    public PatchMapper<GroupRestEntity> groupRestEntityPatchMapper(ObjectMapper objectMapper) {
        return new GenericPatchMapper<>(objectMapper, GroupRestEntity.class);
    }

    @Bean
    public PatchMapper<OrganizationRestEntity> organizationRestEntityPatchMapper(ObjectMapper objectMapper) {
        return new GenericPatchMapper<>(objectMapper, OrganizationRestEntity.class);
    }

    @Bean
    public PatchMapper<CountryRestEntity> countryRestEntityPatchMapper(ObjectMapper objectMapper) {
        return new GenericPatchMapper<>(objectMapper, CountryRestEntity.class);
    }

    @Bean
    public PatchMapper<ChannelRestEntity> channelRestEntityPatchMapper(ObjectMapper objectMapper) {
        return new GenericPatchMapper<>(objectMapper, ChannelRestEntity.class);
    }

    @Bean
    public PatchMapper<AppPermissionRestEntity> appPermissionRestEntityPatchMapper(ObjectMapper objectMapper) {
        return new GenericPatchMapper<>(objectMapper, AppPermissionRestEntity.class);
    }

    @Bean
    public PatchMapper<AppRoleRestEntity> appRoleRestEntityPatchMapper(ObjectMapper objectMapper) {
        return new GenericPatchMapper<>(objectMapper, AppRoleRestEntity.class);
    }

    // Provide a Jackson Module that registers the BigDecimalPlainSerializer.
    // Registering a Module bean is non-invasive and lets Spring Boot auto-register other modules
    // (e.g. JavaTimeModule from jackson-datatype-jsr310) so OffsetDateTime is handled correctly.
    @Bean
    public Module bigDecimalPlainModule() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(BigDecimal.class, new BigDecimalPlainSerializer());
        return module;
    }

    // Explicitly provide JavaTimeModule to ensure java.time types (e.g. OffsetDateTime) are supported
    // even if auto-registration does not occur for some reason.
    @Bean
    public JavaTimeModule javaTimeModule() {
        return new JavaTimeModule();
    }
}
