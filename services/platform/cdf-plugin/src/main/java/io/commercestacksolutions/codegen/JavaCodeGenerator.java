package io.commercestacksolutions.codegen;

import io.commercestacksolutions.codegen.model.CdfConstructor;
import io.commercestacksolutions.codegen.model.CdfDefinition;
import io.commercestacksolutions.codegen.model.CdfField;
import io.commercestacksolutions.codegen.model.CdfMethod;
import io.commercestacksolutions.codegen.model.CdfMethodParameter;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generates Java source code from a (merged) {@link CdfDefinition}.
 *
 * <p>For every field the generator automatically produces a getter and setter unless
 * a method with the same name is already explicitly defined in the CDF.
 */
public class JavaCodeGenerator {

    private static final String INDENT = "    ";

    /**
     * Generate the full Java source file content from the given definition.
     *
     * @param definition the (possibly merged) CDF definition
     * @return Java source code as a string
     */
    public String generate(CdfDefinition definition) {
        StringBuilder sb = new StringBuilder();

        // Package
        sb.append("package ").append(definition.getPkg()).append(";\n\n");

        // Imports
        for (String imp : definition.getImports()) {
            sb.append("import ").append(imp).append(";\n");
        }
        if (!definition.getImports().isEmpty()) {
            sb.append("\n");
        }

        // Class-level annotations
        for (String ann : definition.getClassAnnotations()) {
            sb.append(ann).append("\n");
        }

        // Class declaration
        sb.append("public class ").append(definition.getEntity());
        if (definition.getSuperClass() != null && !definition.getSuperClass().isEmpty()) {
            sb.append(" extends ").append(definition.getSuperClass());
        }
        List<String> ifaces = definition.getInterfaces();
        if (!ifaces.isEmpty()) {
            sb.append(" implements ").append(String.join(", ", ifaces));
        }
        sb.append(" {\n\n");

        // Fields
        for (CdfField field : definition.getFields()) {
            for (String ann : field.getAnnotations()) {
                sb.append(INDENT).append(ann).append("\n");
            }
            sb.append(INDENT).append("private ").append(field.getType()).append(" ").append(field.getName());
            if (field.getInitialValue() != null && !field.getInitialValue().isEmpty()) {
                sb.append(" = ").append(field.getInitialValue());
            }
            sb.append(";\n\n");
        }

        // Constructors
        Set<String> explicitConstructors = definition.getConstructors().stream()
                .map(this::constructorParamSignature)
                .collect(Collectors.toSet());

        // Always output explicit constructors from CDF
        for (CdfConstructor ctor : definition.getConstructors()) {
            appendConstructor(sb, definition.getEntity(), ctor);
        }

        // If no constructors defined at all, emit an empty default constructor
        if (explicitConstructors.isEmpty()) {
            sb.append(INDENT).append("public ").append(definition.getEntity()).append("() {\n");
            sb.append(INDENT).append("}\n\n");
        }

        // Auto-generated getters and setters (skip if method already defined in CDF)
        Set<String> explicitMethodNames = definition.getMethods().stream()
                .map(CdfMethod::getName)
                .collect(Collectors.toSet());

        for (CdfField field : definition.getFields()) {
            String getterName = getterName(field);
            String setterName = setterName(field);

            if (!explicitMethodNames.contains(getterName)) {
                appendGetter(sb, field);
            }
            if (!explicitMethodNames.contains(setterName)) {
                appendSetter(sb, field);
            }
        }

        // Custom methods from CDF
        for (CdfMethod method : definition.getMethods()) {
            appendMethod(sb, method);
        }

        sb.append("}\n");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void appendConstructor(StringBuilder sb, String entityName, CdfConstructor ctor) {
        sb.append(INDENT).append(ctor.getVisibility()).append(" ").append(entityName).append("(");
        List<CdfMethodParameter> params = ctor.getParameters();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(params.get(i).getType()).append(" ").append(params.get(i).getName());
        }
        sb.append(") {\n");
        String body = ctor.getBody();
        if (body != null && !body.isBlank()) {
            sb.append(INDENT).append(INDENT).append(body).append("\n");
        }
        sb.append(INDENT).append("}\n\n");
    }

    private void appendGetter(StringBuilder sb, CdfField field) {
        String name = getterName(field);
        sb.append(INDENT).append("public ").append(field.getType()).append(" ").append(name).append("() {\n");
        sb.append(INDENT).append(INDENT).append("return ").append(field.getName()).append(";\n");
        sb.append(INDENT).append("}\n\n");
    }

    private void appendSetter(StringBuilder sb, CdfField field) {
        String name = setterName(field);
        sb.append(INDENT).append("public void ").append(name).append("(")
                .append(field.getType()).append(" ").append(field.getName()).append(") {\n");
        sb.append(INDENT).append(INDENT).append("this.").append(field.getName())
                .append(" = ").append(field.getName()).append(";\n");
        sb.append(INDENT).append("}\n\n");
    }

    private void appendMethod(StringBuilder sb, CdfMethod method) {
        for (String ann : method.getAnnotations()) {
            sb.append(INDENT).append(ann).append("\n");
        }
        sb.append(INDENT).append(method.getVisibility()).append(" ")
                .append(method.getReturnType()).append(" ").append(method.getName()).append("(");

        List<CdfMethodParameter> params = method.getParameters();
        for (int i = 0; i < params.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(params.get(i).getType()).append(" ").append(params.get(i).getName());
        }
        sb.append(") {\n");

        String body = method.getBody();
        if (body != null && !body.isBlank()) {
            sb.append(INDENT).append(INDENT).append(body).append("\n");
        }
        sb.append(INDENT).append("}\n\n");
    }

    private String getterName(CdfField field) {
        String rawType = field.getType();
        boolean isBooleanType = "boolean".equals(rawType) || "Boolean".equals(rawType);
        String prefix = isBooleanType ? "is" : "get";
        return prefix + capitalize(field.getName());
    }

    private String setterName(CdfField field) {
        return "set" + capitalize(field.getName());
    }

    private String capitalize(String name) {
        if (name == null || name.isEmpty()) return name;
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    private String constructorParamSignature(CdfConstructor ctor) {
        return ctor.getParameters().stream()
                .map(CdfMethodParameter::getType)
                .collect(Collectors.joining(","));
    }
}
