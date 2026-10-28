package io.commercestacksolutions.coreserviceapp.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan(basePackages = "io.commercestacksolutions.coreserviceapp")
@EntityScan(basePackages = "io.commercestacksolutions.coreserviceapp.dataaccess")
public class CoreServiceAppAutoConfiguration {
}
