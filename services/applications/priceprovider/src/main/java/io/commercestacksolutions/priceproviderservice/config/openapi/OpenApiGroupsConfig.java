package io.commercestacksolutions.priceproviderservice.config.openapi;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiGroupsConfig {

    @Bean
    public GroupedOpenApi publicApiGroup() {
        return GroupedOpenApi.builder()
                .group("public-api")
                .pathsToMatch("/public/api/**")
                .build();
    }

    @Bean
    public GroupedOpenApi adminApiGroup() {
        return GroupedOpenApi.builder()
                .group("admin-api")
                .pathsToMatch("/admin/api/**")
                .pathsToExclude("/public/api/**")
                .build();
    }
}
