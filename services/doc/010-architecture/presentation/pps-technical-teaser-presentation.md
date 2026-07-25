---
marp: true
theme: gaia
_class: lead
paginate: true
backgroundColor: #f4f6f9
color: #333333
style: |
  section {
    font-family: 'Segoe UI', Arial, sans-serif;
    padding: 40px;
  }
  h1 {
    color: #0d47a1;
  }
  h2 {
    color: #1b5e20;
  }
  footer {
    font-size: 0.55em;
    color: #777;
  }
  code {
    background-color: #eceff1;
    color: #b71c1c;
  }
  section.lead h1 {
    font-size: 2.1em;
    color: #0d47a1;
  }
  section.lead h3 {
    color: #555;
  }
---

# Composable Commerce Starters
### Price Provider Service - Technical Teaser zur 1.0.0 Beta

<br>

**Architektur, Agentic Engineering und die Zukunft modularer E-Commerce-Systeme**

<!--
* STIMMUNG & TONFALL:
  - Hochgradig professionell, technisch versiert, begeisternd und visionär.
  - Ein "Deep Dive" für Architekten, Lead-Entwickler und CTOs.

* VISUALS IM VIDEO:
  - Einblenden des Titels mit technischem Hintergrund (z. B. fließende Binärdaten oder ein sauberes Schichtenmodell-Gitter).
  - Minimalistische, futuristische Musik im Hintergrund.

* SPRECHZETTEL / TALKING POINTS:
  - Hallo und herzlich willkommen zum technischen Teaser für den anstehenden 1.0.0 Beta-Release des Price Provider Service!
  - Wenn Sie genug von monolithischen Black-Box-Lösungen und Vendor-Lock-ins haben, sind Sie hier genau richtig.
  - Heute blicken wir tief unter die Haube unserer Composable Commerce Starters. Wir zeigen Ihnen, wie wir DDD, Agentic Engineering und deklarative Codegenerierung verbinden, um ein absolut kompromissloses Pricing-Backend bereitzustellen.
  - Machen Sie sich bereit für einen echten Architektur-Deep-Dive!
-->

---

## Composable Commerce Starters

*Ein vollständig lauffähiges Open-Source-Starter-Kit mit agentischer Codebasis.*

### Das Versprechen:
* **Schnelle Umsetzung**: Sofort startfähiger, produktionsreifer Technologiestack.
* **Domain-Driven-Architektur (DDD)**: Skalierbar für hochkomplexe Commerce-Szenarien.
* **Full Control**: Vollständiger Quellcode in Ihrer Hand. Keine Black-Box-Logik, keine versteckten Lizenzgebühren.

### Das Prinzip:
> "Bauen Sie Ihren eigenen Service auf Basis standardisierter, hochqualitativer Core-Plattformmodule auf."

<!--
* STIMMUNG & TONFALL:
  - Überzeugend, stark, souverän.
  - Fokus auf Freiheit und Kontrolle ("Full Control").

* VISUALS IM VIDEO:
  - Symbole für "Open Source" (z. B. offenes Schloss), "Skalierbarkeit" (wachsende Knotenpunkte) und "Code" (Terminal-Icon).
  - Betonung des Slogans "Full Control" mit einer eleganten Text-Animation.

* SPRECHZETTEL / TALKING POINTS:
  - Was verbirgt sich hinter den "Composable Commerce Starters"? Es ist nicht einfach nur ein Template, sondern ein vollständig lauffähiges, hochmodernes Open-Source-Starter-Kit.
  - Das Problem heute ist oft: Entweder man baut alles mühsam von Null auf, oder man kauft teure SaaS-Lösungen ein, die man nicht anpassen kann und die einen Vendor-Lock-in bedeuten.
  - Wir geben Ihnen die volle Kontrolle zurück. Der Quellcode gehört Ihnen, die Architektur ist modular nach DDD-Prinzipien aufgebaut und bereit für komplexe Enterprise-Szenarien.
-->

---

## DDD & Modulare Architektur

*Der Price Provider Service nutzt eine strikte Trennung zwischen wiederverwendbaren Plattformkomponenten und der eigentlichen Anwendungsdomäne.*

```
   +-------------------------------------------------------------+
   |                     Price Provider App                      |
   +-------------------------------------------------------------+
          | (Nutzt und erweitert)
          v
   +-------------------------------------------------------------+
   |  Platform-Module: corebusiness & coreserviceapp             |
   |  (Enthält Stammdaten wie Currency, Unit, TaxClass, Security) |
   +-------------------------------------------------------------+
```

* **Plattform-Kernelemente**: `coreserviceapp` (OIDC, Security) und `corebusinessentities` (Währungen, Einheiten, Sprachen).
* **Fachliche Domäne**: Der Price Provider integriert diese Module und fügt spezifische Preisdatenbanken (`PriceRowEntity`) hinzu.

<!--
* STIMMUNG & TONFALL:
  - Fachmännisch, fokussiert auf Software-Design.
  - Langsames Erklären der Modulgrenzen.

* VISUALS IM VIDEO:
  - Grafischer Aufbau der beiden Schichten.
  - Die Plattform-Module leuchten blau, die Applikations-Schicht grün.
  - Einblendung von Dateipfaden wie `services/platform/` vs. `services/applications/`.

* SPRECHZETTEL / TALKING POINTS:
  - Schauen wir uns die Modulstruktur genauer an. Nach den Prinzipien des Domain-Driven Designs trennen wir wiederverwendbare Plattformkonzepte von der konkreten Anwendung.
  - Unter `platform/` finden Sie unsere Core-Module: `coreserviceapp` für die gesamte Sicherheitsinfrastruktur und `corebusinessentities` für fundamentale E-Commerce-Stammdaten wie Währungen, Einheiten und Steuerklassen.
  - Die eigentliche Applikation unter `applications/priceprovider` ist schlank. Sie referenziert diese Core-Module und erweitert sie um die konkrete Pricing-Fachlogik wie Preiszeilen und Mengenstaffeln.
  - Das bedeutet für Sie: Sie können auf dieser soliden Plattform im Handumdrehen völlig eigene Microservices aufbauen.
-->

---

## Die Schichtenarchitektur (Layered Architecture)

*Jedes Modul folgt einer konsequenten, sauberen Schichttrennung nach SOLID-Prinzipien und Interface-Driven Design (IDD).*

```
   [ Controller ] ---> [ Facade (DTOs) ] ---> [ Service (IDD) ] ---> [ Data Access ]
```

* **Data Access Layer**: JPA-Repositories und deklarative Abfragen (PostgreSQL / H2).
* **Service Layer**: Kapselung der reinen Business-Logik hinter Interfaces (IDD). Keine Vermischung mit HTTP- oder DB-Konzepten. Fully unit-testable.
* **Facade Layer**: DTO-Mapping und dynamische Expansionen (z. B. `$expand` für verknüpfte Entitäten).
* **Controller Layer**: REST-Schnittstellen mit sauberer OpenAPI-Dokumentation.

<!--
* STIMMUNG & TONFALL:
  - Präzise, qualitätsbewusst.
  - Vermittlung von hoher Softwarequalität.

* VISUALS IM VIDEO:
  - Die Schichten werden als fließendes Diagramm animiert.
  - Bei "Service Layer" blinkt ein Zahnrad-Symbol für Business Logik.
  - Ein "Unit-Test" Symbol erscheint über dem Service Layer, um die leichte Testbarkeit durch Mocking der Interfaces zu demonstrieren.

* SPRECHZETTEL / TALKING POINTS:
  - Softwarequalität entscheidet über die langfristige Wartbarkeit. Deshalb setzen wir auf eine klassische, saubere Schichtenarchitektur.
  - Der Datenfluss läuft strikt unidirektional: Vom REST-Controller über die Facade-Ebene, die das DTO-Mapping und komplexe Daten-Expansionen steuert, in den Service-Layer.
  - Die Services sind komplett nach dem Interface-Driven Design (IDD) aufgebaut. Sie sind absolut frei von HTTP-Frameworks oder direkter Datenbankkopplung.
  - Dadurch können Sie jede Komponente isoliert testen, mocken oder austauschen. Ein Traum für jeden QA- und DevOps-Engineer!
-->

---

## Headless & Composable Integration

*Eine reinrassige API-First-Lösung für maximale Systemunabhängigkeit.*

* **REST-API**: Vollständig entkoppelt. Bereit zur Integration in Ihr bestehendes ERP (z. B. SAP), PIM oder moderne Shopsysteme (Commercetools, Shopify).
* **Standardisiertes API-Verhalten**: Konsistente Fehlercodes, Filterungen und Pagination.
* **Angular Admin-App**: Ein modernes, standalone-basiertes Angular-Frontend wird für die Stammdatenpflege direkt mitgeliefert.
* **Dynamic Form Generation**: Das Frontend rendert Eingabemasken dynamisch zur Laufzeit über Metadaten.

<!--
* STIMMUNG & TONFALL:
  - Dynamisch, integrationsfreudig.
  - Betonung der Flexibilität im "Composable" Ökosystem.

* VISUALS IM VIDEO:
  - Der Price Provider im Zentrum mit Verbindungen zu verschiedenen Systemen (ERP, PIM, Webshop, Admin-App).
  - Ein kurzer Einblick in die Angular-Admin-App (ein stilisiertes Dashboard-UI).

* SPRECHZETTEL / TALKING POINTS:
  - Headless und Composable sind für uns keine Buzzwords, sondern gelebter Standard.
  - Die REST-API des Price Providers ist vollständig entkoppelt. Es spielt keine Rolle, ob Sie ein Enterprise-PIM anbinden, Echtzeit-Abfragen aus einem ERP-System durchführen oder Ihr eigenes Custom-Frontend bedienen.
  - Für Administratoren liefern wir eine moderne Angular-Admin-App mit.
  - Der Clou: Diese App ist hochdynamisch und baut Formulare zur Laufzeit über Metadaten auf. Das reduziert den UI-Entwicklungsaufwand für neue Felder auf exakt Null.
-->

---

## Security: Keycloak, RBAC & Permission Selectors

*Sicherheit auf Enterprise-Niveau: Vom Identity Provider bis zur feingranularen Datenzeile.*

### 1. Keycloak OIDC-Authentifizierung
* Token-basierte Absicherung aller administrativen und öffentlichen Endpunkte.
* Automatisierte Extraktion hierarchischer Organisationen (B2B-Gruppen) aus JWT-Claims.

### 2. Role-Based Access Control (RBAC)
* Vordefinierte Rollen wie `Superuser`, `Contributor` oder `PriceRowReader` für passgenaue Rechteprofile.

### 3. Dynamic Permission Selectors
* Datenbankseitige Filterung basierend auf fachlichen Ausdrücken:
  ```
  priceprovider.admin:PriceRow[currencyRef=='EUR']:write
  ```
* Verhindert das unbefugte Lesen oder Schreiben sensibler Daten (z. B. Einkaufspreise) direkt auf SQL-Ebene!

<!--
* STIMMUNG & TONFALL:
  - Hochgradig sicherheitsbewusst, ernsthaft, überzeugend.
  - Betonung der Einzigartigkeit der "Permission Selectors".

* VISUALS IM VIDEO:
  - Ein Keycloak-Logo links, das ein JWT-Token emittiert.
  - Ein stilisierter SQL-Query-Generator, der zeigt, wie der "Permission Selector" dynamisch ein `WHERE` Statement in die Datenbank injiziert.
  - Hervorheben des Ausdrucks `currencyRef=='EUR'` in einer Code-Box.

* SPRECHZETTEL / TALKING POINTS:
  - Ein echtes Highlight der 1.0.0 Beta ist unser Sicherheitskonzept.
  - Wir integrieren standardmäßig Keycloak über OpenID Connect. Das JWT-Token wird nicht nur zur Authentifizierung genutzt, sondern wir extrahieren daraus auch tief verschachtelte B2B-Organisationsstrukturen.
  - Über das RBAC-System steuern wir Rollen und Rechte. Aber das reicht im B2B-Bereich oft nicht aus.
  - Deshalb haben wir "Permission Selectors" entwickelt. Hiermit können Sie Berechtigungen direkt an Bedingungen knüpfen.
  - Zum Beispiel: Ein Preismanager darf nur Datensätze bearbeiten, wenn die Währung "EUR" ist. Das System wertet diesen Ausdruck aus und filtert die Daten direkt auf Datenbankebene!
  - Das ist maximale Sicherheit bei überragender Performance.
-->

---

## Extension Strategy & Code Generation (CDF)

*Wie erweitern Partner das Datenmodell, ohne den Core-Code zu forken?*

* **Class Definition Format (CDF)**: Entity- und DTO-Strukturen werden in deklarativen JSON-Dateien definiert.
* **Build-Time Compilation**: Unser Gradle-Plugin (`cdf-plugin`) scannt alle JSON-Dateien, verschmilzt Kern-Strukturen mit Partner-Erweiterungen und generiert reinen Java-Quellcode.
* **Maximale Performance**: Keine langsame Laufzeit-Reflektion, kein Hibernate-Mapping-Overhead. Reines, schnelles Java compile-time compile.

```json
{
  "entity": "ChannelEntity",
  "fields": [
    { "name": "customPartnerField", "type": "String", "annotations": ["@Column"] }
  ]
}
```

<!--
* STIMMUNG & TONFALL:
  - Technisch tiefgründig, architektonisch begeistert.
  - Erklärung des "No-Fork"-Konzepts.

* VISUALS IM VIDEO:
  - Visualisierung des Merging-Prozesses:
    [ Base CDF JSON ] + [ Partner Extension JSON ] ---> (Gradle Build cdf-plugin) ---> [ Generated Java Entity class ]
  - Eine Animation von Zahnrädern, die perfekt ineinandergreifen, ohne den bestehenden Code zu verändern.

* SPRECHZETTEL / TALKING POINTS:
  - Jetzt kommen wir zur Entwickler-Magie: Unsere Erweiterungsstrategie über das "Class Definition Format", kurz CDF.
  - In klassischen Systemen müssen Sie den Quellcode forken oder mühsame Vererbungsketten in JPA aufbauen, was zu massiven Performance-Problemen führt.
  - Unsere Lösung: Wir definieren Entities deklarativ als JSON.
  - Während des Gradle-Builds nimmt unser `cdf-plugin` die Basis-Definitionen der Plattform und verschmilzt sie mit Ihren individuellen Erweiterungen.
  - Das Ergebnis? Sauber generierte Java-Klassen in Ihrem Build-Verzeichnis. Kein XML-Overhead, keine träge Laufzeit-Reflektion, volle Typsicherheit und perfekte Upgrade-Pfade für kommende Releases!
-->

---

## Agentic Engineering Ready

*Das erste Pricing-Framework, das nativ für die Zusammenarbeit mit KI-Agenten konzipiert wurde.*

### Vordefinierte KI-Agent-Skills:
1. **Fullstack Entity-Erstellung**: Generierung neuer Datenmodelle von der DB bis zum UI via CDF.
2. **Query-Filtering**: Automatische Bereitstellung mächtiger Such- und Filterschnittstellen auf Basis von Such-Prompts.
3. **RBAC & i18n Skills**: KI-gestützte Pflege von Sicherheitsrollen und Lokalisierungen.

### Der Workflow:
> Sie beschreiben das gewünschte Feature als Freitext im GitHub Copilot oder Cursor Agent. Der KI-Agent nutzt die vordefinierten Skills, erstellt die CDF-Dateien, generiert den Code und baut die Anwendung fehlerfrei neu.

<!--
* STIMMUNG & TONFALL:
  - Futuristisch, bahnbrechend, enthusiastisch.
  - Zeigt, dass wir die Entwicklungseffizienz neu definieren.

* VISUALS IM VIDEO:
  - Ein stilisierter Prompt-Eingabebereich mit blinkendem Cursor: "/create-entity name=Voucher code=String validTo=DateTime"
  - Eine Grafik, die zeigt, wie der KI-Agent die Arbeit von Stunden in Sekunden erledigt und die fertigen Dateien in die Ordnerstruktur einsortiert.

* SPRECHZETTEL / TALKING POINTS:
  - Die 1.0.0 Beta des Price Providers ist "Agentic Engineering Ready". Was heißt das?
  - Wir haben spezielle Entwickler-Skills im Repository hinterlegt. KI-Agenten wie GitHub Copilot Workspace oder Cursor können diese direkt einlesen.
  - Wenn Sie ein neues Feature brauchen – wie zum Beispiel ein Rabatt-Guthaben-System –, beschreiben Sie das einfach als Text-Prompt.
  - Der KI-Agent versteht dank unserer Skills sofort die Schichtenarchitektur, das CDF-Format und die REST-Schnittstellen. Er generiert die JSON-Definitionen, passt die UI an und testet den Code.
  - Das ist keine Science-Fiction, das ist die reale Entwickler-Arbeit mit unserem Composable Commerce Starter.
-->

---

## Delivery Starters: Bereit für die Cloud

*Keine halben Sachen: Wir liefern das komplette Deployment-Ökosystem direkt mit.*

* **Docker Compose Stack**: Starten Sie die gesamte Plattform (Price Provider, dynamic Angular UI, Keycloak, PostgreSQL) lokal mit nur einem Befehl:
  ```bash
  docker-compose up -d
  ```
* **Production-Ready Dockerfiles**: Optimierte Multi-Stage Builds basierend auf **OpenJDK 25** und schlanken Linux-Images.
* **Infrastructure as Code (IaC)**: Vorkonfigurierte Setup-Skripte für ein schnelles, automatisiertes Deployment in **Azure Container Apps** (ACA).

<!--
* STIMMUNG & TONFALL:
  - Praktisch, pragmatisch, "DevOps-freundlich".
  - Vermittlung von Betriebssicherheit und einfacher Bereitstellung.

* VISUALS IM VIDEO:
  - Das Terminal mit dem Befehl `docker-compose up -d` zeigt einen schnellen Start aller Services (grüne Haken für "Started").
  - Das Azure-Logo und ein schematisches Cloud-Kubernetes-Netzwerk.

* SPRECHZETTEL / TALKING POINTS:
  - Ein hervorragender Service nützt nichts, wenn sein Deployment Tage dauert.
  - Deshalb liefern wir die "Delivery Starters" mit.
  - Für die lokale Entwicklung nutzen Sie einfach unseren Docker Compose Stack. Ein einziger Befehl startet die Datenbank, das Identity-Management, das Backend und das Frontend.
  - Für den Ernstfall in der Cloud bieten wir fertig optimierte Dockerfiles auf Basis von Java 25 sowie vorbereitete Deployment-Skripte für Azure Container Apps.
  - Damit bringen Sie Ihre Pricing-Infrastruktur innerhalb weniger Minuten in eine hochverfügbare, skalierbare Cloud-Umgebung!
-->

---

## Das erwartet Sie im 1.0.0 Beta-Release:

* **Stabile, hochperformante APIs** für den produktiven Einsatz.
* **Umfassende Performance-Optimierungen** für rekursive B2B-Preisfindungsabfragen in PostgreSQL.
* **Erweiterte Permission Selectors** für noch feingranulareren Datenzugriff.
* **Vollständige Dokumentation** aller Erweiterungs- und Integrations-Szenarien.

<br>

### Werden Sie Teil der Beta-Phase!
> "Testen Sie die Grenzen des modernen Pricings. Geben Sie uns Feedback auf GitHub und gestalten Sie die Zukunft des Composable Commerce mit uns."

<!--
* STIMMUNG & TONFALL:
  - Motivierend, einladend, gemeinschaftsorientiert.
  - Ein starker Schlussakkord.

* VISUALS IM VIDEO:
  - Das "1.0.0 Beta" Logo glänzt im Zentrum.
  - Kontaktdaten, GitHub-Repository-Link und ein QR-Code zum Scannen.
  - Sprecher lächelt (falls im Video zu sehen) oder verabschiedet sich mit einer einladenden Geste.

* SPRECHZETTEL / TALKING POINTS:
  - Die Veröffentlichung der Version 1.0.0 Beta des Price Provider Service steht kurz bevor!
  - Sie erhalten eine extrem stabile, produktionsreife Codebasis mit maximaler Performance, bahnbrechender Flexibilität durch das CDF-Plugin und native Vorbereitung auf KI-Agenten.
  - Wir laden Sie herzlich ein: Werden Sie Teil unserer Beta-Phase. Laden Sie sich das Starter-Kit herunter, probieren Sie es aus und teilen Sie Ihr Feedback mit uns auf GitHub.
  - Vielen Dank für Ihre Zeit und viel Spaß beim Entwickeln der nächsten Generation von E-Commerce-Lösungen! Bis bald!
-->
