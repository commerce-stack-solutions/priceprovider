package de.ebusyness;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(scanBasePackages = {
        "de.ebusyness.priceproviderservice",
        "de.ebusyness.commons.dataaccess.dbupdate",
        "de.ebusyness.commons.dataaccess.meta",
        "de.ebusyness.commons.dataaccess.config",
        "de.ebusyness.commons.service"
})
@EnableAsync
public class PriceProviderServiceApp {
    public static void main(String[] args) {
        SpringApplication.run(PriceProviderServiceApp.class, args);
    }
}