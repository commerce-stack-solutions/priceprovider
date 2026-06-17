package io.commercestacksolutions.commons.dataaccess.meta;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Base class for registries that manage dynamic enum value definitions.
 *
 * @param <T> the type of the value object
 * @param <D> the type of the definition bean
 */
public abstract class EnumTypeValueRegistry<T, D extends EnumTypeValueDefinition<T>> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<String, D> definitions = new HashMap<>();
    private final Function<T, String> codeExtractor;

    protected EnumTypeValueRegistry(List<D> definitions, Function<T, String> codeExtractor) {
        this.codeExtractor = codeExtractor;
        if (definitions == null) {
            logger.warn("{} initialized with no definitions (null).", getClass().getSimpleName());
            return;
        }

        for (D d : definitions) {
            if (d == null || d.getValue() == null) {
                logger.error("Definition or its value is null, skipping: {}", d);
                continue;
            }
            String code = codeExtractor.apply(d.getValue());
            if (code == null) {
                logger.error("Value code is null for definition: {}", d);
                continue;
            }

            if (this.definitions.containsKey(code)) {
                logger.error("Duplicate code found: {}. Skipping bean: {}", code, d);
            } else {
                this.definitions.put(code, d);
            }
        }

        if (this.definitions.isEmpty()) {
            logger.warn("{} initialized with no definitions.", getClass().getSimpleName());
        }
    }

    public boolean exists(String code) {
        return code != null && definitions.containsKey(code.toUpperCase());
    }

    public D get(String code) {
        return code == null ? null : definitions.get(code.toUpperCase());
    }

    public List<String> getCodes() {
        return List.copyOf(definitions.keySet());
    }

    public List<D> getDefinitions() {
        return List.copyOf(definitions.values());
    }
}
