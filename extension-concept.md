# Extension Concept: Product Refactoring for Partner Extensibility

## 1. Executive Summary
The goal of this refactoring is to enable partners to extend the core product (Service & App) without modifying the original source code, adhering strictly to the **Open-Closed Principle**.

The concept revolves around a **Metadata-Driven Architecture** where:
1. **Entities** are defined via a descriptive format and generated at build time.
2. **Service Logic** is extensible through an Interceptor/Hook system.
3. **UI** is dynamically rendered based on expanded metadata from the backend.

---

## 2. Service Layer Refactoring

### 2.1 Entity Definition Format (EDF) & Code Generation
Instead of hardcoding JPA entities in Java, entities will be described in a **JSON or XML Entity Definition Format (EDF)**.

**Approach:**
- **Core Product** provides base EDF files (e.g., `PriceRow.edf.json`).
- **Partners** provide extension EDF files (e.g., `PriceRow.ext.json`) containing additional fields.
- **Build Process:** A custom Gradle plugin merges these files (fail-fast on field name conflicts) and uses a **Freemarker** template to generate the final `PriceRowEntity.java` class.

**Example `PriceRow.ext.json`:**
```json
{
  "entity": "PriceRow",
  "fields": [
    {
      "name": "barcode",
      "type": "String",
      "annotations": ["@Column(length = 50)", "@MandatoryField"],
      "ui": {
        "label": "fields.barcode",
        "type": "text",
        "sortOrder": 105
      }
    }
  ]
}
```

### 2.2 Service Layer Hooks (Interceptor System)
To extend business logic without touching core services, we introduce a **Service Interceptor Pattern** leveraging Spring AOP or simple Bean collection.

**Mechanism:**
Every core service (implementing `EntityService<T>`) will execute a chain of interceptors during the `save` and `delete` cycles.

```java
public interface EntityServiceInterceptor<T> {
    default void beforeSave(T entity, T existingEntity) { }
    default void afterSave(T entity) { }
    default void beforeDelete(String id) { }
}
```

**Refactored `performGenericSave`:**
```java
public default <ID> T performGenericSave(T entity) {
    T existing = fetchAndDetachExistingEntity(...);

    // Partner Hook 1: Before Save
    interceptors.forEach(i -> i.beforeSave(entity, existing));

    validateEntity(entity);
    updateAuditTimestamps(entity);

    T saved = getRepository().save(entity);

    // Partner Hook 2: After Save
    interceptors.forEach(i -> i.afterSave(saved));

    return saved;
}
```

### 2.3 Expanded `$meta` API
The current `$meta` API only provides basic structural info. It will be expanded to include UI-centric metadata, allowing the frontend to render forms automatically.

**Expanded `MetaInfo` Object:**
```json
{
  "identityFields": ["id"],
  "fields": [
    {
      "name": "priceValue",
      "type": "number",
      "mandatory": true,
      "labelKey": "common.fields.priceValue",
      "uiType": "currency",
      "sortOrder": 10
    },
    {
      "name": "barcode",
      "type": "string",
      "mandatory": true,
      "labelKey": "fields.barcode",
      "uiType": "text",
      "sortOrder": 105
    }
  ]
}
```

---

## 3. App Layer Refactoring

### 3.1 Backend-Driven Navigation & Tiles
The "Startpage Tiles" and "Main Menu" will no longer be hardcoded in Angular. Instead, a new endpoint `/api/ui/config` will provide the layout.

**Response Example:**
```json
{
  "menu": [
    { "label": "Prices", "route": "/pricerows", "icon": "bi-tags", "order": 10 },
    { "label": "Loyalty Points", "route": "/loyalty", "icon": "bi-star", "order": 100 }
  ],
  "tiles": [
    { "title": "Manage Prices", "route": "/pricerows", "color": "blue" }
  ]
}
```

### 3.2 Dynamic Form Component
The core of the UI extensibility is the `DynamicFormComponent`. Instead of static templates (like `pricerow-form.component.html`), this component iterates over the fields provided by the `$meta` API.

**Sketch of `DynamicFormComponent`:**
```typescript
@Component({
  selector: 'app-dynamic-form',
  template: `
    <form [formGroup]="form">
      <div *ngFor="let field of meta().fields" class="row mb-3">
        <label class="col-sm-3 col-form-label">{{ field.labelKey | transloco }}</label>
        <div class="col-sm-9">
          <!-- Render different inputs based on field.uiType -->
          <input *ngIf="field.uiType === 'text'" [formControlName]="field.name" class="form-control">
          <app-enum-selector *ngIf="field.uiType === 'enum'" ...></app-enum-selector>
          <app-reference-edit *ngIf="field.uiType === 'reference'" ...></app-reference-edit>
        </div>
      </div>
    </form>
  `
})
export class DynamicFormComponent {
  // Logic to build FormGroup dynamically from MetaInfo
}
```

---

## 4. Implementation Details & Examples

### 4.1 Entity Descriptor Notation (EDN)
The project will use a standardized JSON notation for defining and extending entities.

**`PriceRow.edf.json` (Core):**
```json
{
  "entity": "PriceRowEntity",
  "tableName": "price_rows",
  "fields": [
    {
      "name": "priceValue",
      "type": "BigDecimal",
      "annotations": ["@Column(precision = 19, scale = 2)", "@MandatoryField"],
      "ui": { "type": "currency", "order": 10 }
    },
    {
      "name": "currencyRef",
      "type": "CurrencyEntity",
      "relation": "ManyToOne",
      "ui": { "type": "reference", "dataSource": "currencies", "order": 20 }
    }
  ]
}
```

### 4.2 Gradle Generation Task
A custom Gradle task `generateEntities` will be responsible for the transformation.

```groovy
task generateEntities {
    doLast {
        def coreFiles = fileTree('src/main/resources/edf/core').include('*.json')
        def extFiles = fileTree('src/main/resources/edf/ext').include('*.json')

        coreFiles.each { coreFile ->
            def entityName = coreFile.name.replace('.edf.json', '')
            def extFile = extFiles.find { it.name == "${entityName}.ext.json" }

            def mergedModel = modelMerger.merge(coreFile, extFile)
            templateEngine.render('Entity.java.ftl', mergedModel, "src/generated/java/.../${entityName}Entity.java")
        }
    }
}
```

### 4.3 Service Interceptor Sample
Implementation of a partner-specific business rule.

```java
@Component
public class LoyaltyPointInterceptor implements EntityServiceInterceptor<PriceRowEntity> {

    @Override
    public void beforeSave(PriceRowEntity entity, PriceRowEntity existing) {
        // Example: If price > 100, automatically set a custom 'loyaltyBonus' field
        if (entity.getPriceValue().compareTo(new BigDecimal("100")) > 0) {
            entity.setAdditionalAttribute("loyaltyBonus", "HIGH_VALUE");
        }
    }
}
```

### 4.4 Angular Dynamic Form Sketch
The `DynamicFormComponent` uses a `FieldRegistry` to map `uiType` from the backend to specific Angular components.

```typescript
// dynamic-form.component.ts
export class DynamicFormComponent implements OnInit {
  @Input() meta!: MetaInfo;
  formGroup: FormGroup = new FormGroup({});

  ngOnInit() {
    this.meta.fields.sort((a, b) => a.order - b.order).forEach(field => {
      this.formGroup.addControl(field.name, new FormControl(
        '', field.mandatory ? Validators.required : null
      ));
    });
  }
}
```

## 5. Developer Experience (DX)

### 5.1 Combined Build Process
The project structure will support a "Combined Build" where the partner project includes the product core as a library.

1. **`product-core.jar`**: Contains logic, services, and base EDF files.
2. **`partner-extension.jar`**: Contains partner EDF files and Interceptor implementations.
3. **`final-application.jar`**: The result of the merge & generation process.

### 4.2 Partner Workflow
1. **Define Extension:** Add new fields in a `*.ext.json` file.
2. **Implement Logic:** Create a class implementing `EntityServiceInterceptor`.
3. **Run Build:** The Gradle task generates/updates entities and compiles everything into a single Spring Boot executable.
4. **Automated UI:** The Angular app detects the new fields via the `$meta` API and renders them automatically in the corresponding forms and tiles.
