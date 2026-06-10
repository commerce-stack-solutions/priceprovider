package io.commercestacksolutions.priceproviderservice.domain.pricetype;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

public record PriceType(@JsonValue String code) {
    @JsonCreator
    public PriceType {
        Objects.requireNonNull(code, "code must not be null");
    }

    @Override
    public String toString() {
        return code;
    }
}
