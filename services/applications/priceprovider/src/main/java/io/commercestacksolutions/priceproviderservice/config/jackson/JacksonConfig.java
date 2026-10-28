package io.commercestacksolutions.priceproviderservice.config.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.commercestacksolutions.priceproviderservice.facade.config.BigDecimalPlainSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.math.BigDecimal;

@Configuration
public class JacksonConfig implements WebMvcConfigurer {

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        SimpleModule bigDecimalModule = new SimpleModule();
        bigDecimalModule.addSerializer(BigDecimal.class, new BigDecimalPlainSerializer());
        objectMapper.registerModule(bigDecimalModule);

        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return objectMapper;
    }

    /**
     * Registers Jackson2JsonNodeConverter (Jackson 2.x) with the highest priority so that
     * @RequestBody parameters typed as com.fasterxml.jackson.databind.JsonNode are deserialized
     * correctly. Spring Boot 4.x defaults to the Jackson 3.x JacksonJsonHttpMessageConverter which
     * cannot handle Jackson 2.x types (used by zjsonpatch for JSON Patch operations).
     */
    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        builder.configureMessageConvertersList(converters ->
                converters.add(0, new Jackson2JsonNodeConverter(objectMapper())));
    }
}
