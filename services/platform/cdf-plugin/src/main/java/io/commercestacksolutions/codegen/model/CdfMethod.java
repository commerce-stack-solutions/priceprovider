package io.commercestacksolutions.codegen.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a method declaration in a CDF class definition.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CdfMethod {

    private String name;

    private List<String> annotations = new ArrayList<>();

    private String visibility = "public";

    private String returnType = "void";

    private List<CdfMethodParameter> parameters = new ArrayList<>();

    private String body = "";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getAnnotations() {
        return annotations;
    }

    public void setAnnotations(List<String> annotations) {
        this.annotations = annotations != null ? annotations : new ArrayList<>();
    }

    public String getVisibility() {
        return visibility != null ? visibility : "public";
    }

    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    public String getReturnType() {
        return returnType != null ? returnType : "void";
    }

    public void setReturnType(String returnType) {
        this.returnType = returnType;
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
