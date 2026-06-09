package io.commercestacksolutions.priceproviderservice.domain.pricetype;

import java.util.Objects;

public record PriceType(String code) {
    public PriceType {
        Objects.requireNonNull(code, "code must not be null");
    }

    @Override
    public String toString() {
        return code;
    }
}
