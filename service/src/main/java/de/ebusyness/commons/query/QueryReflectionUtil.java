package de.ebusyness.commons.query;

import jakarta.persistence.Id;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper utilities used by the query/specification builder.
 */
public final class QueryReflectionUtil {

    private QueryReflectionUtil() {}

    /**
     * Attempts to find the name of the id attribute on the given entity class using reflection.
     * Returns null if no id attribute could be detected.
     */
    public static String findIdAttributeName(Class<?> entityClass) {
        // Search fields for @Id
        for (Field f : entityClass.getDeclaredFields()) {
            if (f.isAnnotationPresent(Id.class)) {
                return f.getName();
            }
        }
        // Search methods (getters) for @Id
        for (Method m : entityClass.getDeclaredMethods()) {
            if (m.isAnnotationPresent(Id.class)) {
                String name = m.getName();
                if (name.startsWith("get") && name.length() > 3) {
                    return Character.toLowerCase(name.charAt(3)) + name.substring(4);
                }
                if (name.startsWith("is") && name.length() > 2) {
                    return Character.toLowerCase(name.charAt(2)) + name.substring(3);
                }
                return name;
            }
        }
        // Fallback common property names
        try {
            entityClass.getDeclaredField("id");
            return "id";
        } catch (NoSuchFieldException ignored) {}
        try {
            entityClass.getDeclaredField(entityClass.getSimpleName().substring(0,1).toLowerCase() + entityClass.getSimpleName().substring(1) + "Id");
            return entityClass.getSimpleName().substring(0,1).toLowerCase() + entityClass.getSimpleName().substring(1) + "Id";
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Convert a string value into the expected target type when possible.
     */
    public static Object convertValueToType(Object value, Class<?> targetType) {
        if (value == null) return null;
        if (targetType.isInstance(value)) return value;
        if (value instanceof String) {
            String s = (String) value;
            try {
                if (targetType == Long.class || targetType == long.class) return Long.valueOf(s);
                if (targetType == Integer.class || targetType == int.class) return Integer.valueOf(s);
                if (targetType == BigDecimal.class) return new BigDecimal(s);
                if (targetType == Boolean.class || targetType == boolean.class) return Boolean.valueOf(s);
                if (targetType == String.class) return s;
            } catch (Exception e) {
                // ignore conversion errors and fall through
            }
        }
        return value;
    }

    /**
     * Builds a simple field name -> type map for the provided entity class.
     * Only top-level properties are considered. The map contains boxed types for primitives.
     */
    public static Map<String, Class<?>> buildFieldTypeMap(Class<?> entityClass) {
        Map<String, Class<?>> map = new HashMap<>();
        if (entityClass == null) return map;

        // Fields
        for (Field f : entityClass.getDeclaredFields()) {
            Class<?> t = boxType(f.getType());
            map.put(f.getName(), t);
        }

        // Getter methods
        for (Method m : entityClass.getDeclaredMethods()) {
            if (m.getParameterCount() != 0) continue;
            String name = m.getName();
            String prop = null;
            if (name.startsWith("get") && name.length() > 3) {
                prop = Character.toLowerCase(name.charAt(3)) + name.substring(4);
            } else if (name.startsWith("is") && name.length() > 2) {
                prop = Character.toLowerCase(name.charAt(2)) + name.substring(3);
            }
            if (prop != null) {
                Class<?> t = boxType(m.getReturnType());
                map.putIfAbsent(prop, t);
            }
        }

        return map;
    }

    private static Class<?> boxType(Class<?> t) {
        if (!t.isPrimitive()) return t;
        if (t == int.class) return Integer.class;
        if (t == long.class) return Long.class;
        if (t == boolean.class) return Boolean.class;
        if (t == double.class) return Double.class;
        if (t == float.class) return Float.class;
        if (t == short.class) return Short.class;
        if (t == byte.class) return Byte.class;
        if (t == char.class) return Character.class;
        return t;
    }
}
