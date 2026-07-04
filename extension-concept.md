# Extension Concept: Product Refactoring for Partner Extensibility

## 1. Executive Summary
The goal of this refactoring is to enable partners to extend the core product (Service & App) without modifying the original source code, adhering strictly to the **Open-Closed Principle**.

The concept revolves around a **Metadata-Driven Architecture** where:
1. **Entities** are defined via a descriptive format and generated at build time.
2. **Business Logic** is extensible through an Interceptor/Hook system.
3. **API Layers** (Facade/Controller) are generic and dynamic.
4. **UI** is dynamically rendered based on expanded metadata from the backend.

---

## 2. Service Layer Refactoring

### 2.1 Entity Definition Format (EDF) & Code Generation
Instead of hardcoding JPA entities in Java, entities are described in a **JSON Entity Definition Format (EDF)**.

**Key EDF Features:**
- **Import Management**: Explicit declaration of required imports.
- **Class-Level Annotations**: Annotations like `@Entity`, `@Table`, or `@JsonIgnoreProperties`.
- **Annotation-Based Relations**: Standard JPA annotations (e.g., `@ManyToOne`, `@ManyToMany`) on fields.
- **Method Management**: Lifecycle hooks (`@PrePersist`) and logic methods defined in the EDF.

**Build Process:**
A custom Gradle task `mergeAndGenerate` will merge core and partner EDF files and use **Apache Freemarker** to generate the final Java source code.

### 2.2 Full Reference: PriceRowEntity EDF
The following is the complete EDF for the existing `PriceRowEntity`, demonstrating that the notation supports all current features including imports, class-level annotations, complex relations, and lifecycle methods.

```json
{
  "entity": "PriceRowEntity",
  "package": "io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.entity",
  "imports": [
    "com.fasterxml.jackson.annotation.JsonIgnoreProperties",
    "io.commercestacksolutions.commons.dataaccess.entity.AuditableEntity",
    "io.commercestacksolutions.commons.dataaccess.idgenerator.GeneratedId",
    "io.commercestacksolutions.commons.dataaccess.idgenerator.IdGeneratorProvider",
    "io.commercestacksolutions.commons.dataaccess.meta.MandatoryField",
    "io.commercestacksolutions.priceproviderservice.dataaccess.channel.entity.ChannelEntity",
    "io.commercestacksolutions.priceproviderservice.dataaccess.currency.entity.CurrencyEntity",
    "io.commercestacksolutions.commons.dataaccess.meta.MetaDynamicEnum",
    "io.commercestacksolutions.priceproviderservice.dataaccess.group.entity.GroupEntity",
    "io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.pricetype.converter.PriceTypeConverter",
    "io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.pricetype.PriceType",
    "io.commercestacksolutions.priceproviderservice.dataaccess.pricerow.pricetype.PriceTypeDefinition",
    "io.commercestacksolutions.priceproviderservice.dataaccess.taxclass.entity.TaxClassEntity",
    "io.commercestacksolutions.priceproviderservice.dataaccess.unit.entity.UnitEntity",
    "jakarta.persistence.*",
    "org.springframework.format.annotation.DateTimeFormat",
    "java.math.BigDecimal",
    "java.time.OffsetDateTime",
    "java.util.HashSet",
    "java.util.Set",
    "java.util.stream.Collectors"
  ],
  "classAnnotations": [
    "@Entity",
    "@JsonIgnoreProperties({\"hibernateLazyInitializer\", \"handler\"})"
  ],
  "interfaces": ["AuditableEntity"],
  "fields": [
    {
      "name": "id",
      "type": "String",
      "annotations": ["@Id", "@GeneratedId", "@Column(length = 100)"]
    },
    {
      "name": "pricedResourceId",
      "type": "String",
      "annotations": ["@MandatoryField"],
      "ui": { "type": "text", "order": 1 }
    },
    {
      "name": "priceValue",
      "type": "BigDecimal",
      "annotations": ["@Column(precision = 19, scale = 2)", "@MandatoryField"],
      "ui": { "type": "currency", "order": 2 }
    },
    {
      "name": "minQuantity",
      "type": "BigDecimal",
      "annotations": ["@Column(precision = 19, scale = 2)", "@MandatoryField"],
      "ui": { "type": "number", "order": 3 }
    },
    {
      "name": "unitRef",
      "type": "UnitEntity",
      "annotations": [
        "@ManyToOne(fetch = FetchType.LAZY)",
        "@JoinColumn(name = \"unit_symbol\")",
        "@MandatoryField"
      ],
      "ui": { "type": "reference", "dataSource": "units", "order": 4 }
    },
    {
      "name": "currencyRef",
      "type": "CurrencyEntity",
      "annotations": [
        "@ManyToOne(fetch = FetchType.LAZY)",
        "@JoinColumn(name = \"currency_key\")",
        "@MandatoryField"
      ],
      "ui": { "type": "reference", "dataSource": "currencies", "order": 5 }
    },
    {
      "name": "taxClassRef",
      "type": "TaxClassEntity",
      "annotations": [
        "@ManyToOne(fetch = FetchType.LAZY)",
        "@JoinColumn(name = \"tax_class_id\", nullable = true)",
        "@MandatoryField"
      ],
      "ui": { "type": "reference", "dataSource": "taxclasses", "order": 6 }
    },
    {
      "name": "priceType",
      "type": "PriceType",
      "annotations": [
        "@Convert(converter = PriceTypeConverter.class)",
        "@MetaDynamicEnum(beanType = PriceTypeDefinition.class)"
      ],
      "ui": { "type": "enum", "order": 7 }
    },
    {
      "name": "validFrom",
      "type": "OffsetDateTime",
      "annotations": ["@DateTimeFormat(pattern = \"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'\")"],
      "ui": { "type": "datetime", "order": 8 }
    },
    {
      "name": "validTo",
      "type": "OffsetDateTime",
      "ui": { "type": "datetime", "order": 9 }
    },
    {
      "name": "groupRefs",
      "type": "Set<GroupEntity>",
      "initialValue": "new HashSet<>()",
      "annotations": [
        "@ManyToMany(fetch = FetchType.LAZY)",
        "@JoinTable(name = \"price_row_groups\", joinColumns = @JoinColumn(name = \"price_row_id\"), inverseJoinColumns = @JoinColumn(name = \"group_id\"))"
      ],
      "ui": { "type": "referencelist", "dataSource": "groups", "order": 10 }
    },
    {
      "name": "channelRefs",
      "type": "Set<ChannelEntity>",
      "initialValue": "new HashSet<>()",
      "annotations": [
        "@ManyToMany(fetch = FetchType.LAZY)",
        "@JoinTable(name = \"price_row_channels\", joinColumns = @JoinColumn(name = \"price_row_id\"), inverseJoinColumns = @JoinColumn(name = \"channel_id\"))"
      ],
      "ui": { "type": "referencelist", "dataSource": "channels", "order": 11 }
    },
    {
      "name": "taxIncluded",
      "type": "boolean",
      "ui": { "type": "boolean", "order": 12 }
    },
    {
      "name": "createdAt",
      "type": "OffsetDateTime"
    },
    {
      "name": "lastModifiedAt",
      "type": "OffsetDateTime"
    }
  ],
  "methods": [
    {
      "name": "prePersist",
      "annotations": ["@PrePersist"],
      "visibility": "protected",
      "body": "if (this.id == null) { this.id = IdGeneratorProvider.generate(PriceRowEntity.class); }"
    },
    {
        "name": "toString",
        "annotations": ["@Override"],
        "visibility": "public",
        "returnType": "String",
        "body": "return \"PriceRowEntity{id=\" + id + \", value=\" + priceValue + \"}\";"
    }
  ]
}
```

### 2.3 Service Layer Interceptors (Spring AOP)
Partners can implement `EntityServiceInterceptor<T>` and register them as Spring Beans.

**Interceptor Interface:**
```java
public interface EntityServiceInterceptor<T> {
    boolean supports(Class<?> entityClass);
    default void beforeSave(T entity, T existingEntity) { }
    default void afterSave(T entity) { }
}
```

**Spring AOP Aspect Implementation:**
```java
@Aspect
@Component
public class ServiceInterceptorAspect {
    @Autowired
    private List<EntityServiceInterceptor<Object>> interceptors;

    @Around("execution(* io.commercestacksolutions.commons.service.entity.EntityService+.save(..))")
    public Object aroundSave(ProceedingJoinPoint joinPoint) throws Throwable {
        Object entity = joinPoint.getArgs()[0];
        EntityService<Object> service = (EntityService<Object>) joinPoint.getTarget();

        Object existing = service.fetchAndDetachExistingEntity(
            service.extractEntityId(entity), service.getRepository(), service.getEntityManager()
        );

        // Execute 'before' hooks
        interceptors.stream()
            .filter(i -> i.supports(entity.getClass()))
            .forEach(i -> i.beforeSave(entity, existing));

        Object result = joinPoint.proceed();

        // Execute 'after' hooks
        interceptors.stream()
            .filter(i -> i.supports(entity.getClass()))
            .forEach(i -> i.afterSave(result));

        return result;
    }
}
```

### 2.4 Facade & Controller Layer Refactoring
To avoid creating new controllers for every extended entity, we move to a **Generic Controller Pattern**.

**Generic Controller:**
```java
@RestController
@RequestMapping("/admin/api/{entityType}")
public class GenericAdminController {
    @Autowired
    private ServiceRegistry serviceRegistry;

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable String entityType, @PathVariable String id) {
        EntityService<?> service = serviceRegistry.get(entityType);
        return ResponseEntity.ok(service.findById(id));
    }

    @PatchMapping(value = "/{id}", consumes = "application/json-patch+json")
    public ResponseEntity<?> patch(@PathVariable String entityType, @PathVariable String id, @RequestBody JsonPatch patch) {
        // Generic patching logic using Service + Mapper
    }
}
```

---

## 3. App Layer Refactoring

### 3.1 Dynamic UI Navigation
The "Startpage Tiles" and "Main Menu" are delivered via `/api/ui/config`.

**Response JSON Schema:**
```json
{
  "navigation": {
    "menu": [
      { "id": "prices", "label": "nav.prices", "route": "/pricerows", "icon": "bi-tags", "order": 10 },
      { "id": "loyalty", "label": "nav.loyalty", "route": "/loyalty", "icon": "bi-star", "order": 100 }
    ],
    "tiles": [
      { "id": "price-mgmt", "title": "tiles.prices", "desc": "tiles.prices.desc", "route": "/pricerows", "color": "blue" }
    ]
  }
}
```

### 3.2 Angular Dynamic Form & Field Registry
The `DynamicFormComponent` uses a registry to render fields based on metadata.

**Field Registry Concept:**
```typescript
const FIELD_COMPONENTS = {
  'text': TextFieldComponent,
  'currency': CurrencyFieldComponent,
  'reference': ReferenceEditComponent,
  'enum': EnumSelectorComponent
};
```

**Dynamic Form Template (Simplified):**
```html
<form [formGroup]="form">
  <ng-container *ngFor="let field of meta().fields">
    <div class="field-wrapper">
      <label>{{ field.labelKey | transloco }}</label>
      <!-- Dynamic Component Injection -->
      <ng-container *ngComponentOutlet="getComponent(field.uiType);
                         inputs: { control: form.get(field.name), config: field }">
      </ng-container>
    </div>
  </ng-container>
</form>
```

---

## 4. Developer Experience (DX) & Workflow

1. **Core Distribution**: Distributed as a JAR including base EDF files in `META-INF/edf/`.
2. **Partner Project**:
   - Adds `extension.edf.json` in `src/main/resources/edf/`.
   - Implements `EntityServiceInterceptor`.
   - The Gradle task `mergeAndGenerate` runs automatically before `compileJava`.
3. **Runtime**:
   - The expanded `$meta` API reports new fields.
   - The Angular App adapts its layout and forms instantly.
