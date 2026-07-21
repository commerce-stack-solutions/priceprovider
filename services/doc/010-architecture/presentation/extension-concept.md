---
marp: true
theme: gaia
_class: lead
paginate: true
backgroundColor: #1e1e24
color: #fff
style: |
  section {
    font-family: 'Helvetica Neue', Arial, sans-serif;
    padding: 40px;
  }
  h1 {
    color: #4fc3f7;
  }
  h2 {
    color: #81c784;
  }
  footer {
    font-size: 0.5em;
    color: #aaa;
  }
---

# Extensible Microservice Platform
### Build-Time Entity Extension via Class Definition Format (CDF)

<center>

![width:450px](assets/slide1.svg)

</center>

---

## 2. Traditional Extensions vs. CDF

How do we extend core platform classes at the application level?

* **Traditional Approaches:**
  * *Polymorphism / Class Inheritance*: Slow runtime reflection, complex Hibernate schemas, tight coupling.
  * *Forking*: Destroys platform upgrade paths; high maintenance overhead.
* **The CDF Build-Time Pattern:**
  * Core entities and REST representation classes are declared as **JSON descriptors**.
  * The custom `cdf-plugin` merges platform base files with application extensions.
  * Generates normal compiled Java classes at compile-time with **zero runtime overhead**.

<center>

![width:600px](assets/slide2.svg)

</center>

---

## 3. The CDF JSON Format & Schema

Classes are declared in highly structured, easy-to-read JSON descriptors.

- **Structural Metadata:** Define target entity name, package path, imports, class annotations, implemented interfaces, and superClass.
- **Fields & Types:** Getters/setters auto-generated. Custom initializers & annotations allowed.
- **Constructors & Methods:** Direct injection of visibilities, parameter types, and raw Java bodies.

<center>

![width:600px](assets/slide3.svg)

</center>

---

## 4. Build-Time Merging Architecture

We resolve platform-to-application dependency limits with **Approach B**:

1. CDF definitions are located in their respective platform modules.
2. The `cdf-codegen` plugin runs **only** in the application service (`priceprovider`).
3. It scans all JSON definitions, merges extensions, and outputs them centrally.
4. Platforms map package-filtered slices of the generated folder to their source sets.

<center>

![width:650px](assets/slide4.svg)

</center>

---

## 5. Summary & Best Practices

An extremely powerful pattern for developer agility and modularity.

* **Introduce New CDF:**
  * Define `{Name}Entity.json` and `{Name}RestEntity.json` in target platform modules.
  * Delete the original manual `.java` files from `src/main/java`.
* **Add Contextual Extension:**
  * Create `{Name}Entity.json` extension under `priceprovider` resources.
  * List only the new fields, custom imports, and customized `toString()` overrides.
* **Keep Build Clean:**
  * Generated files reside cleanly inside the ignored `build/` workspace.
  * Disabling JVM Class Data Sharing (`-Xshare:off`) silences warnings across tests.

<center>

![width:350px](assets/slide5.svg)

</center>
