package io.commercestacksolutions.codegen.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a constructor declaration in a CDF class definition.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CdfConstructor {

    private String visibility = "public";

    private List<CdfMethodParameter> parameters = new ArrayList<>();

    private String body = "";

    public String getVisibility() {
        return visibility != null ? visibility : "public";
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public List<CdfMethodParameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<CdfMethodParameter> parameters) {
        this.parameters = parameters != null ? parameters : new ArrayList<>();
    }

    public String getBody() {
        return body != null ? body : "";
    }

    public void setBody(String body) {
        this.body = body;
    }
}
