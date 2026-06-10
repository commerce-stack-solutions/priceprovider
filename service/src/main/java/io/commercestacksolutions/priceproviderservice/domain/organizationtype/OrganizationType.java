package io.commercestacksolutions.priceproviderservice.domain.organizationtype;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Objects;

public record OrganizationType(@JsonValue String code) {
    @JsonCreator
    public OrganizationType {
        Objects.requireNonNull(code, "code must not be null");
    }

    @Override
    public String toString() {
        return code;
    }
}
