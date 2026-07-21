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
<svg width="450" height="200" viewBox="0 0 450 200" fill="none" xmlns="http://www.w3.org/2000/svg">
  <!-- Core Box -->
  <rect x="20" y="50" width="130" height="80" rx="10" fill="#2d3748" stroke="#4fc3f7" stroke-width="3"/>
  <text x="85" y="90" fill="#4fc3f7" font-family="Arial" font-size="14" font-weight="bold" text-anchor="middle">Base Entity</text>
  <text x="85" y="110" fill="#a0aec0" font-family="Arial" font-size="11" text-anchor="middle">Platform Module</text>

  <!-- Plus Sign -->
  <text x="185" y="100" fill="#fff" font-family="Arial" font-size="30" font-weight="bold" text-anchor="middle">+</text>

  <!-- Extension Box -->
  <rect x="220" y="50" width="130" height="80" rx="10" fill="#2d3748" stroke="#81c784" stroke-width="3"/>
  <text x="285" y="90" fill="#81c784" font-family="Arial" font-size="14" font-weight="bold" text-anchor="middle">Extension</text>
  <text x="285" y="110" fill="#a0aec0" font-family="Arial" font-size="11" text-anchor="middle">Application</text>

  <!-- Arrow -->
  <path d="M 365 90 L 395 90" stroke="#fff" stroke-width="3" marker-end="url(#arrow)"/>
  <defs>
    <marker id="arrow" viewBox="0 0 10 10" refX="5" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
      <path d="M 0 0 L 10 5 L 0 10 z" fill="#fff"/>
    </marker>
  </defs>

  <!-- Merged Output Box -->
  <rect x="410" y="40" width="30" height="100" rx="5" fill="#319795" />
  <text x="425" y="95" fill="#fff" font-family="Arial" font-size="12" font-weight="bold" text-anchor="middle" transform="rotate(-90 425 95)">Merged</text>
</svg>
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
<svg width="600" height="120" viewBox="0 0 600 120" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect x="10" y="10" width="260" height="100" rx="8" fill="#5a2328" stroke="#feb2b2" stroke-width="2"/>
  <text x="140" y="40" fill="#feb2b2" font-family="Arial" font-size="14" font-weight="bold" text-anchor="middle">Traditional Runtime</text>
  <text x="140" y="65" fill="#fff" font-family="Arial" font-size="11" text-anchor="middle">Reflection, slower boot, leaky DB mappings,</text>
  <text x="140" y="85" fill="#fff" font-family="Arial" font-size="11" text-anchor="middle">and high maintenance overhead</text>

  <rect x="330" y="10" width="260" height="100" rx="8" fill="#1c4532" stroke="#9ae6b4" stroke-width="2"/>
  <text x="460" y="40" fill="#9ae6b4" font-family="Arial" font-size="14" font-weight="bold" text-anchor="middle">Mata-Driven CDF</text>
  <text x="460" y="65" fill="#fff" font-family="Arial" font-size="11" text-anchor="middle">Build-time merging, compile-time checks,</text>
  <text x="460" y="85" fill="#fff" font-family="Arial" font-size="11" text-anchor="middle">fully decoupled, 100% clean JARs</text>
</svg>
</center>

---

## 3. The CDF JSON Format & Schema

Classes are declared in highly structured, easy-to-read JSON descriptors.

- **Structural Metadata:** Define target entity name, package path, imports, class annotations, implemented interfaces, and superClass.
- **Fields & Types:** Getters/setters auto-generated. Custom initializers & annotations allowed.
- **Constructors & Methods:** Direct injection of visibilities, parameter types, and raw Java bodies.

<center>
<svg width="600" height="150" viewBox="0 0 600 150" fill="none" xmlns="http://www.w3.org/2000/svg">
  <!-- JSON Box -->
  <rect x="50" y="10" width="160" height="130" rx="8" fill="#2d3748" stroke="#cbd5e0" stroke-width="2"/>
  <text x="130" y="35" fill="#cbd5e0" font-family="Arial" font-size="13" font-weight="bold" text-anchor="middle">JSON Schema</text>
  <text x="130" y="65" fill="#fff" font-family="Arial" font-size="11" text-anchor="middle">- package, imports</text>
  <text x="130" y="85" fill="#fff" font-family="Arial" font-size="11" text-anchor="middle">- fields, annotations</text>
  <text x="130" y="105" fill="#fff" font-family="Arial" font-size="11" text-anchor="middle">- methods, ctors</text>

  <!-- Arrow -->
  <path d="M 230 75 L 350 75" stroke="#4fc3f7" stroke-width="3" marker-end="url(#arrow-blue)"/>
  <defs>
    <marker id="arrow-blue" viewBox="0 0 10 10" refX="5" refY="5" markerWidth="6" markerHeight="6" orient="auto-start-reverse">
      <path d="M 0 0 L 10 5 L 0 10 z" fill="#4fc3f7"/>
    </marker>
  </defs>
  <text x="290" y="60" fill="#4fc3f7" font-family="Arial" font-size="11" font-weight="bold" text-anchor="middle">cdf-plugin</text>

  <!-- Java Box -->
  <rect x="380" y="10" width="170" height="130" rx="8" fill="#2d3748" stroke="#4fc3f7" stroke-width="2"/>
  <text x="465" y="35" fill="#4fc3f7" font-family="Arial" font-size="13" font-weight="bold" text-anchor="middle">Java Source Class</text>
  <text x="465" y="65" fill="#fff" font-family="Arial" font-size="11" text-anchor="middle">- standard JPA entity</text>
  <text x="465" y="85" fill="#fff" font-family="Arial" font-size="11" text-anchor="middle">- fields & fields accessors</text>
  <text x="465" y="105" fill="#fff" font-family="Arial" font-size="11" text-anchor="middle">- custom overridden methods</text>
</svg>
</center>

---

## 4. Build-Time Merging Architecture

We resolve platform-to-application dependency limits with **Approach B**:

1. CDF definitions are located in their respective platform modules.
2. The `cdf-codegen` plugin runs **only** in the application service (`priceprovider`).
3. It scans all JSON definitions, merges extensions, and outputs them centrally.
4. Platforms map package-filtered slices of the generated folder to their source sets.

<center>
<svg width="650" height="180" viewBox="0 0 650 180" fill="none" xmlns="http://www.w3.org/2000/svg">
  <!-- Core Business JSON -->
  <rect x="10" y="20" width="140" height="50" rx="5" fill="#2d3748" stroke="#e2e8f0" stroke-width="2"/>
  <text x="80" y="45" fill="#fff" font-family="Arial" font-size="10" text-anchor="middle">Base Entities CDF</text>
  <text x="80" y="58" fill="#e2e8f0" font-family="Arial" font-size="8" text-anchor="middle">(corebusinessentities)</text>

  <!-- Extension JSON -->
  <rect x="10" y="100" width="140" height="50" rx="5" fill="#2d3748" stroke="#e2e8f0" stroke-width="2"/>
  <text x="80" y="125" fill="#fff" font-family="Arial" font-size="10" text-anchor="middle">Extension CDF</text>
  <text x="80" y="138" fill="#e2e8f0" font-family="Arial" font-size="8" text-anchor="middle">(priceproviderservice)</text>

  <!-- Merger Task -->
  <rect x="200" y="55" width="180" height="60" rx="8" fill="#2b6cb0" stroke="#90cdf4" stroke-width="2"/>
  <text x="290" y="85" fill="#fff" font-family="Arial" font-size="11" font-weight="bold" text-anchor="middle">Generate Task (Merger)</text>
  <text x="290" y="100" fill="#90cdf4" font-family="Arial" font-size="9" text-anchor="middle">priceprovider build folder</text>

  <!-- Platform Source Sets Map -->
  <rect x="440" y="55" width="190" height="60" rx="8" fill="#2f855a" stroke="#9ae6b4" stroke-width="2"/>
  <text x="535" y="80" fill="#fff" font-family="Arial" font-size="11" font-weight="bold" text-anchor="middle">Package-Filtered Mapping</text>
  <text x="535" y="95" fill="#9ae6b4" font-family="Arial" font-size="9" text-anchor="middle">Exclude other modules' packages</text>
  <text x="535" y="105" fill="#9ae6b4" font-family="Arial" font-size="8" text-anchor="middle">Compile safely with zero circular deps</text>

  <!-- Connectors -->
  <path d="M 150 45 L 200 70" stroke="#cbd5e0" stroke-width="2"/>
  <path d="M 150 125 L 200 100" stroke="#cbd5e0" stroke-width="2"/>
  <path d="M 380 85 L 440 85" stroke="#cbd5e0" stroke-width="2" stroke-dasharray="4"/>
</svg>
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
<svg width="350" height="80" viewBox="0 0 350 80" fill="none" xmlns="http://www.w3.org/2000/svg">
  <rect x="10" y="10" width="330" height="60" rx="5" fill="#2d3748" stroke="#4fc3f7" stroke-width="2"/>
  <text x="175" y="35" fill="#4fc3f7" font-family="Arial" font-size="12" font-weight="bold" text-anchor="middle">100% Modularity & Agility</text>
  <text x="175" y="55" fill="#fff" font-family="Arial" font-size="10" text-anchor="middle">Upgrade platform modules without breaking app schemas</text>
</svg>
</center>
