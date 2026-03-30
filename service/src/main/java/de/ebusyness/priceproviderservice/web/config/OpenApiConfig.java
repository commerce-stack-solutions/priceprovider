package de.ebusyness.priceproviderservice.web.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI priceProviderOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Price Provider Service API")
                        .description("REST API for managing units and price rows. Supports pagination, sorting, and filtering.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("API Support")
                                .email("support@ebusyness.de")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development server"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local server")
                ));
    }
}
