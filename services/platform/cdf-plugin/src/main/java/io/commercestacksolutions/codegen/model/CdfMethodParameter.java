package io.commercestacksolutions.codegen.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a method parameter in a CDF method definition.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CdfMethodParameter {

    private String name;

    private String type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
