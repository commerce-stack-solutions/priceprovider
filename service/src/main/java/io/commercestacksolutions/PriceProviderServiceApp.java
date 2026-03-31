package io.commercestacksolutions;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
        "io.commercestacksolutions.priceproviderservice",
        "io.commercestacksolutions.commons.dataaccess.dbupdate",
        "io.commercestacksolutions.commons.dataaccess.meta",
        "io.commercestacksolutions.commons.dataaccess.config",
        "io.commercestacksolutions.commons.service"
})
@EnableAsync
public class PriceProviderServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(PriceProviderServiceApp.class, args);
    }
}