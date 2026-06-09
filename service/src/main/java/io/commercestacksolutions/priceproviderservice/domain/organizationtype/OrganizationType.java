package io.commercestacksolutions.priceproviderservice.domain.organizationtype;

import java.util.Objects;

public record OrganizationType(String code) {
    public OrganizationType {
        Objects.requireNonNull(code, "code must not be null");
    }

    @Override
    public String toString() {
        return code;
    }
}
