# Developer Guide: Dynamic Enums

Dynamic Enums (also known as extensible types) are a pattern used in CommerceStack Solutions to provide flexibility for domain concepts that need to be extended by external modules or partners without modifying the core service.

Unlike standard Java enums, Dynamic Enums are implemented as Spring-managed beans and discovered at runtime.

## Core Components

A Dynamic Enum consists of four main parts:

1.  **Value Object**: A Java `record` (or class) representing the type (e.g., `PriceType`).
2.  **Definition Interface**: An interface extending `EnumTypeValueDefinition<T>` that defines the bean type.
3.  **Registry**: A class extending `EnumTypeValueRegistry<T, D>` that manages the available definitions.
4.  **JPA Converter**: An `AttributeConverter` to store the value object in the database as a string.

---

## How to Implement a New Dynamic Enum

If you want to introduce a new extensible type (e.g., `ColorType`):

### 1. Create the Value Object

Define a record to hold the string code. Use `@JsonValue` and `@JsonCreator` for REST API compatibility.

```java
public record ColorType(@JsonValue String code) {
    @JsonCreator
    public ColorType {
        Objects.requireNonNull(code, "code must not be null");
    }

    @Override
    public String toString() {
        return code;
    }
}
```

### 2. Define the Definition Interface

Create an interface for the Spring beans that will define specific values.

```java
public interface ColorTypeDefinition extends EnumTypeValueDefinition<ColorType> {
    ColorType getColorType();

    @Override
    default ColorType getValue() {
        return getColorType();
    }
}
```

### 3. Create the Registry

Create a registry component that extends the generic base class.

```java
@Component
public class ColorTypeRegistry extends EnumTypeValueRegistry<ColorType, ColorTypeDefinition> {
    public ColorTypeRegistry(List<ColorTypeDefinition> definitions) {
        super(definitions, ColorType::code);
    }
}
```

### 4. Implement a JPA Converter

Ensure the value object can be persisted.

```java
@Converter(autoApply = true)
public class ColorTypeConverter implements AttributeConverter<ColorType, String> {
    @Override
    public String convertToDatabaseColumn(ColorType attribute) {
        return attribute == null ? null : attribute.code();
    }

    @Override
    public ColorType convertToEntityAttribute(String dbData) {
        return dbData == null ? null : new ColorType(dbData);
    }
}
```

### 5. Use in Entity

Annotate the field in your entity. Use `@MetaDynamicEnum` to make values available in `$meta` responses.

```java
@Entity
public class ProductEntity {
    @Convert(converter = ColorTypeConverter.class)
    @MetaDynamicEnum(beanType = ColorTypeDefinition.class)
    private ColorType color;
}
```

---

## How to Extend an Existing Dynamic Enum

To add a new value to an existing Dynamic Enum (e.g., adding `PURPLE` to `ColorType`):

1.  **Create a new implementation** of the definition interface.
2.  **Annotate it with `@Component`**.

```java
@Component
public class PurpleColorType implements ColorTypeDefinition {
    @Override
    public ColorType getColorType() {
        return new ColorType("PURPLE");
    }
}
```

That's it! The new value will be automatically:
- Discovered by the `ColorTypeRegistry`.
- Validated if you have a validation rule using the registry.
- Included in the `$meta` response for the API.
- Available in the frontend dropdowns.
