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

# Extensible Platform
### Build-Time Entity Extension via CDF

<center>

![width:450px](assets/slide1.svg)

</center>

<!--
Welcome everyone. Today we are presenting the new Extensible Microservice Platform concept, specifically detailing how we extend database entities and REST facade representations cleanly at build-time using Class Definition Format (CDF) descriptors.
-->

---

## 2. Decoupling the Schema

* **Traditional:** Tight coupling, slow runtime reflection, and schema complexity.
* **CDF Pattern:** Meta-driven, zero runtime overhead, and clean upgrade paths.

<center>

![width:600px](assets/slide2.svg)

</center>

<!--
On this slide, we contrast traditional extension approaches with our build-time CDF pattern.
Instead of using Hibernate polymorphic mappings or runtime reflection which cause significant performance penalties and leak domain concepts across modules, CDF uses a declarative JSON schema. This ensures clean JARs and zero reflection overhead at runtime, while keeping our upgrade paths fully intact.
-->

---

## 3. Declarative JSON Schema

* Class package, target superClass, and imports.
* Accessor fields with annotations.
* Direct injection of custom Java methods & constructors.

<center>

![width:600px](assets/slide3.svg)

</center>

<!--
Here we look at the structure of a CDF file. It is a highly structured JSON file representing all package paths, target class details, annotations, fields, and custom behaviors. Out of these files, standard JPA entity classes and REST RestEntities are generated, fully automated by our build plugin.
-->

---

## 4. Merging Architecture (Approach B)

* Multi-project definitions merged centrally.
* Package-filtered source directories for each subproject.
* Zero compile-time circular dependency loops.

<center>

![width:650px](assets/slide4.svg)

</center>

<!--
This is the core build-time flow. All CDF JSON files across different projects are scanned centrally during the build of our leaf application service (priceprovider).
The plugin merges base schemas with service-specific extensions. It then outputs them back to the platform modules' source sets using precise, package-filtered directory mapping. This successfully keeps our dependencies strictly one-directional.
-->

---

## 5. Developer Guide Summary

* **Add Entity:** Define JSON descriptor in platform module & delete manual Java.
* **Add Extension:** List only new fields & imports in priceprovider.
* **Prise Build:** Centralized and clean generated workspace under `build/`.

<center>

![width:350px](assets/slide5.svg)

</center>

<!--
To summarize, introducing or extending a class is incredibly simple. For a new entity, you just define the JSON descriptors in the platform, and the plugin takes care of the rest. For extensions, like priceRepresentationMode on ChannelEntity, you define a small extension JSON in the priceprovider service, listing only the fields you are adding. The code is generated seamlessly under the build directory.
-->
