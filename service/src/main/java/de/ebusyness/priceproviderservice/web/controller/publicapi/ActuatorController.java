package de.ebusyness.priceproviderservice.web.controller.publicapi;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/public/actuator")
public class ActuatorController {

    @GetMapping(value = "/health", produces = "application/json")
    public Map<String, String> health() {
        return Collections.singletonMap("status", "UP");
    }
}
