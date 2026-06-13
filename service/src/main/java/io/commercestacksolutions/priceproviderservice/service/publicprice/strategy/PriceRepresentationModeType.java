package io.commercestacksolutions.priceproviderservice.service.publicprice.strategy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

public record PriceRepresentationModeType(@JsonValue String code) {
    @JsonCreator
    public PriceRepresentationModeType {
        Objects.requireNonNull(code, "code must not be null");
    }

    @Override
    public String toString() {
        return code;
    }
}
