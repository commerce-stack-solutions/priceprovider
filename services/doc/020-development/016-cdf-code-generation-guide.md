# Development Guide – Class Definition Format (CDF) & Code Generation

This guide explains the Class Definition Format (CDF), the custom code generation plugin, and how to define, extend, and transition Java classes into JSON-based CDF definitions.

---

## 1. Overview & Motivation

In a multi-module microservice platform, core business concepts (such as `Channel`, `Country`, `Currency`, and `AppRole`) reside in reusable **platform modules** (under `services/platform/`). However, individual **service applications** (such as the `priceprovider`) often need to extend these platform-level entities with context-specific fields and relationships without forking the platform modules.

Standard Java/JPA inheritance strategies (like `@MappedSuperclass` or Hibernate polymorphism) suffer from performance overhead, leaky database abstractions, or tight coupling.

To solve this, Commercestack Solutions uses a metadata-driven **Class Definition Format (CDF)**. Standard Java files are replaced by JSON descriptors. A custom Gradle build plugin (`cdf-plugin`) reads these JSON descriptors, merges base definitions from platform modules with application-level extensions, and generates regular Java source files at build-time.

### Key Benefits
- **Zero Reflection Overhead**: Code generation happens at compile-time; Hibernate scans pure generated Java classes.
- **Clean Extensibility**: Service applications can easily extend base platform entities by defining small, incremental JSON extension files.
- **Centralized Management**: All generated source files stay in the `priceprovider` build folder under `build/generated/sources/cdf/java/` to keep git history clean, and are dynamically mapped to their respective modules' compile classpaths.

---

## 2. The CDF JSON Format

A Class Definition File is a JSON descriptor representing a Java class's structure. Here is the full schema format:

```json
{
  "entity": "ChannelEntity",
  "package": "io.commercestacksolutions.corebusinessentities.dataaccess.channel.entity",
  "imports": [
    "io.commercestacksolutions.commons.dataaccess.entity.AuditableEntity",
    "jakarta.persistence.*",
    "java.time.OffsetDateTime"
  ],
  "classAnnotations": [
    "@Entity",
    "@JsonIgnoreProperties({\"hibernateLazyInitializer\", \"handler\"})"
  ],
  "superClass": "BaseParentClass",
  "interfaces": [
    "AuditableEntity"
  ],
  "fields": [
    {
      "name": "id",
      "type": "String",
      "annotations": [
        "@Id"
      ]
    },
    {
      "name": "priceRepresentationMode",
      "type": "PriceRepresentationModeType",
      "initialValue": "PriceRepresentationModeType.GROSS",
      "annotations": [
        "@Convert(converter = PriceTypeConverter.class)",
        "@MandatoryField"
      ]
    }
  ],
  "constructors": [
    {
      "visibility": "public",
      "parameters": [
        { "name": "id", "type": "String" }
      ],
      "body": "this.id = id;"
    }
  ],
  "methods": [
    {
      "name": "toString",
      "annotations": ["@Override"],
      "visibility": "public",
      "returnType": "String",
      "body": "return \"ChannelEntity{\" + \"id='\" + id + \"'\" + '}';"
    }
  ]
}
```

### Properties Reference

| Property | Type | Description |
|---|---|---|
| `entity` | `String` | **Required.** The name of the class to be generated (e.g. `ChannelEntity`). |
| `package` | `String` | **Required.** The target Java package (e.g. `io.commercestacksolutions.corebusinessentities.dataaccess.channel.entity`). |
| `imports` | `List<String>` | Packages or classes to import in the generated Java file. |
| `classAnnotations` | `List<String>` | Annotations placed at the class level (e.g., `@Entity`, `@Table`). |
| `superClass` | `String` | Optional base class to extend (omitting means no explicit extends). |
| `interfaces` | `List<String>` | List of Java interfaces the class implements. |
| `fields` | `List<Field>` | Definitions of the private class fields. Getters and setters are auto-generated. |
| `constructors` | `List<Constructor>` | Optional custom constructors. If none are specified, an empty default constructor is auto-generated. |
| `methods` | `List<Method>` | Custom methods with visibilities, annotations, parameter structures, and code blocks. |

#### Field Object Properties
- `name`: Name of the private field.
- `type`: Java type (e.g. `String`, `Set<CountryEntity>`).
- `annotations`: List of annotations to be placed above the field declaration.
- `initialValue`: Optional initial value (e.g. `new HashSet<>()`).

#### Constructor Object Properties
- `visibility`: Visibility modifier (`public`, `protected`, `private`).
- `parameters`: List of parameter objects containing `name` and `type`.
- `body`: Raw Java string containing the body statements.

#### Method Object Properties
- `name`: Name of the method.
- `annotations`: Method-level annotations (e.g. `@Override`, `@Transient`).
- `visibility`: Visibility modifier.
- `returnType`: Return type (e.g. `void`, `Set<String>`).
- `parameters`: List of parameter objects.
- `body`: Raw Java string representing the method body.

---

## 3. The Merging Mechanism

When the CDF build task (`generateClassesFromCDF`) is executed, it scans all `src/main/resources/cdf` directories in the workspace. If multiple `.json` files define the same `entity` (i.e. share the same `entity` value), they are merged into a single merged class definition based on the following precise merging rules:

- **Imports**: Union of all imports across definitions (duplicates are removed, order is preserved).
- **Class Annotations**: Union of all class-level annotations.
- **Interfaces**: Union of all implemented interfaces.
- **Super Class**: The base definition's superClass wins. An extension may only define a superClass if the base definition defines none.
- **Fields**: Order of fields in the base definition is preserved; extension fields are appended to the end. If a field name is present in both base and extension:
  - Changing the field's `type` is strictly **forbidden** and will fail the build.
  - New annotations from the extension are merged (union of annotations on that field).
- **Methods**: If an extension defines a method with the exact same name as a base method, the extension's method **completely replaces/overrides** the base method.
- **Constructors**: Deduplicated based on parameter-type signatures; all unique constructors are emitted.

---

## 4. How to Introduce or Transition a Class as CDF

Follow these step-by-step instructions to introduce a new CDF definition or transition a legacy hand-written Java class to CDF:

### Step 1: Create the Base Definition in the Platform Module
1. Navigate to the appropriate platform module directory (e.g., `services/platform/corebusinessentities/`).
2. Create a new directory `src/main/resources/cdf` if it does not exist yet.
3. Add a `{YourEntityName}.json` file defining the core/minimum required fields, constructors, and custom methods.
4. Add the respective REST-facing `{YourEntityName}RestEntity.json` and `{YourEntityName}ListRestEntity.json` files to `src/main/resources/cdf`.
5. Remove the hand-written Java classes from `src/main/java`.

### Step 2: Create the Extension Definition in the Service Application (Optional)
If your service application (e.g. `priceprovider`) needs to add application-specific fields to that platform entity:
1. Navigate to the service application directory (`services/applications/priceprovider/`).
2. Add a `{YourEntityName}.json` file under `src/main/resources/cdf/`.
3. Omit any package-level class annotations or base fields. Specify only the target `entity` name, target `package`, and list the new custom fields, custom imports, or overridden `toString` methods:
   ```json
   {
     "entity": "ChannelEntity",
     "package": "io.commercestacksolutions.corebusinessentities.dataaccess.channel.entity",
     "imports": [
       "io.commercestacksolutions.corebusinessentities.dataaccess.channel.pricerepresentationmode.PriceRepresentationModeType"
     ],
     "fields": [
       {
         "name": "priceRepresentationMode",
         "type": "PriceRepresentationModeType",
         "annotations": [
           "@MandatoryField"
         ]
       }
     ]
   }
   ```

### Step 3: Run Code Generation & Verify Compilation
Run the code generator centrally from the priceprovider service:
```bash
cd services/applications/priceprovider/
./gradlew generateClassesFromCDF compileJava
```
- The generated Java files will appear in the build folder: `priceprovider/build/generated/sources/cdf/java/`.
- Verify that they compile flawlessly across all project sub-modules.
