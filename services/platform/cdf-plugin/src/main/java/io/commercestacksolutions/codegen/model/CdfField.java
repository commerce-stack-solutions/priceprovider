package io.commercestacksolutions.codegen.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a field declaration in a CDF class definition.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CdfField {

    private String name;

    private String type;

    private List<String> annotations = new ArrayList<>();

    private String initialValue;

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

    public List<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations != null ? annotations : new ArrayList<>();
    }

    public String getInitialValue() {
        return initialValue;
    }

    public void setInitialValue(String initialValue) {
        this.initialValue = initialValue;
    }
}
