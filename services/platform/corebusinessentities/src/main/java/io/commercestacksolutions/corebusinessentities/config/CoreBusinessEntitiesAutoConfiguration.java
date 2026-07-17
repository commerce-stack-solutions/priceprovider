package io.commercestacksolutions.corebusinessentities.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan(basePackages = "io.commercestacksolutions.corebusinessentities")
@EntityScan(basePackages = "io.commercestacksolutions.corebusinessentities.dataaccess")
public class CoreBusinessEntitiesAutoConfiguration {
}
