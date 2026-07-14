package io.commercestacksolutions.codegen.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Top-level Class Definition Format (CDF) model.
 * Represents the full definition of a Java class to be generated.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CdfDefinition {

    private String entity;

    @JsonProperty("package")
    private String pkg;

    private List<String> imports = new ArrayList<>();

    private List<String> classAnnotations = new ArrayList<>();

    private String superClass;

    private List<String> interfaces = new ArrayList<>();

    private List<CdfField> fields = new ArrayList<>();

    private List<CdfConstructor> constructors = new ArrayList<>();

    private List<CdfMethod> methods = new ArrayList<>();

    public String getEntity() {
        return entity;
    }

    public void setEntity(String entity) {
        this.entity = entity;
    }

    public String getPkg() {
        return pkg;
    }

    public void setPkg(String pkg) {
        this.pkg = pkg;
    }

    public List<String> getImports() {
        return imports;
    }

    public void setImports(List<String> imports) {
        this.imports = imports != null ? imports : new ArrayList<>();
    }

    public List<String> getClassAnnotations() {
        return classAnnotations;
    }

    public void setClassAnnotations(List<String> classAnnotations) {
        this.classAnnotations = classAnnotations != null ? classAnnotations : new ArrayList<>();
    }

    public String getSuperClass() {
        return superClass;
    }

    public void setSuperClass(String superClass) {
        this.superClass = superClass;
    }

    public List<String> getInterfaces() {
        return interfaces;
    }

    public void setInterfaces(List<String> interfaces) {
        this.interfaces = interfaces != null ? interfaces : new ArrayList<>();
    }

    public List<CdfField> getFields() {
        return fields;
    }

    public void setFields(List<CdfField> fields) {
        this.fields = fields != null ? fields : new ArrayList<>();
    }

    public List<CdfConstructor> getConstructors() {
        return constructors;
    }

    public void setConstructors(List<CdfConstructor> constructors) {
        this.constructors = constructors != null ? constructors : new ArrayList<>();
    }

    public List<CdfMethod> getMethods() {
        return methods;
    }

    public void setMethods(List<CdfMethod> methods) {
        this.methods = methods != null ? methods : new ArrayList<>();
    }
}
