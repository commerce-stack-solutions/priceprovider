package io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.pricetype;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class PriceTypeRegistry {

    private static final Logger logger = LoggerFactory.getLogger(PriceTypeRegistry.class);

    private final Map<String, PriceTypeDefinition> definitions;

    public PriceTypeRegistry(List<PriceTypeDefinition> definitions) {
        this.definitions = new HashMap<>();
        if (definitions == null) {
            logger.warn("PriceTypeRegistry initialized with no definitions (null/ no definitions provided).");
            return;
        }
        for (PriceTypeDefinition d : definitions) {
            if (d == null || d.getPriceType() == null || d.getPriceType().code() == null) {
                logger.error("PriceTypeDefinition or its code is null, skipping: {}", d);
                continue;
            }
            String code = d.getPriceType().code().toUpperCase();
            if (this.definitions.containsKey(code)) {
                logger.error("Duplicate PriceType code found: {}. Skipping bean: {}", code, d);
            } else {
                this.definitions.put(code, d);
            }
        }

        if (this.definitions.isEmpty()) {
            logger.warn("PriceTypeRegistry initialized with no definitions.");
        }
    }

    public boolean exists(String code) {
        return code != null && definitions.containsKey(code.toUpperCase());
    }

    public PriceTypeDefinition get(String code) {
        return definitions.get(code);
    }

    public List<String> getCodes() {
        return List.copyOf(definitions.keySet());
    }
}
