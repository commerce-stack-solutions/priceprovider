package io.commercestacksolutions.corebusinessentities.facade.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.commercestacksolutions.commons.mapper.GenericPatchMapper;
import io.commercestacksolutions.commons.mapper.PatchMapper;
import io.commercestacksolutions.corebusinessentities.facade.channel.restentity.ChannelRestEntity;
import io.commercestacksolutions.corebusinessentities.facade.country.restentity.CountryRestEntity;
import io.commercestacksolutions.corebusinessentities.facade.currency.restentity.CurrencyRestEntity;
import io.commercestacksolutions.corebusinessentities.facade.group.restentity.GroupRestEntity;
import io.commercestacksolutions.corebusinessentities.facade.organization.restentity.OrganizationRestEntity;
import io.commercestacksolutions.corebusinessentities.facade.taxclass.restentity.TaxClassRestEntity;
import io.commercestacksolutions.corebusinessentities.facade.unit.restentity.UnitRestEntity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CoreBusinessEntitiesMapperConfig {

    @Bean
    public PatchMapper<UnitRestEntity> unitRestEntityPatchMapper(ObjectMapper objectMapper) {
        return new GenericPatchMapper<>(objectMapper, UnitRestEntity.class);
    }

    @Bean
    public PatchMapper<TaxClassRestEntity> taxClassRestEntityPatchMapper(ObjectMapper objectMapper) {
        return new GenericPatchMapper<>(objectMapper, TaxClassRestEntity.class);
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
}
