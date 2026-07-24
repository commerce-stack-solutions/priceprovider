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
    font-size: 2.2em;
    color: #0d47a1;
  }
  section.lead h3 {
    color: #555;
  }
---

# Composable Commerce Starter
### Der Price Provider Service als zentrale Preismanagement-Lösung

<br>

**Ein Trainingsvideo für Business Analysten im modernen Retail**

<!--
* STIMMUNG & TONFALL:
  - Professionell, enthusiastisch, einladend und zukunftsorientiert.
  - Klare, deutliche Aussprache. Nicht zu schnell sprechen.

* VISUALS IM VIDEO:
  - Einblenden des Titels mit einer eleganten Einblendanimation.
  - Eventuell eine sanfte Hintergrundmusik, die leiser wird, sobald der Sprecher beginnt.
  - Ein Logo der "Commercestack Solutions" oder ein symbolisches Icon für Cloud-Pricing.

* SPRECHZETTEL / TALKING POINTS:
  - Herzlich willkommen zu diesem Trainingsvideo zum Thema modernes Preismanagement im Retail!
  - Heute richten wir uns speziell an Sie als Business Analysten. Warum? Weil Sie die Brücke zwischen Business-Anforderungen und IT bilden.
  - Wir stellen Ihnen den "Price Provider Service" vor – das Herzstück unseres Composable Commerce Starters für intelligentes Pricing.
  - Ziel des Videos: Nach diesen ca. 7 Minuten werden Sie genau verstehen, wie dieser Service komplexe Preisstrategien für Onlineshops und Vermietungsszenarien spielend leicht löst und wie Sie damit die Time-to-Market drastisch verkürzen können.
-->

---

## Willkommen beim Price Provider Service!

### Was ist der Price Provider Service?
* **Zentraler Open-Source-Microservice** für flexibles Preismanagement in modernen E-Commerce-Architekturen.
* Bereitstellung einer **standardisierten, hochperformanten REST-API**.
* Vollständige Abdeckung von Preisen, Preisstaffeln, Währungen und Einheiten über diverse Verkaufskanäle hinweg.

### Die drei Säulen:
1. **Multi-Channel-Fähigkeit** (B2B & B2C Kanäle trennen)
2. **Kunden- & Organisations-Segmentierung** (Hierarchische B2B-Preise)
3. **Agentic Engineering Ready** (KI-Agenten erweitern den Service per Prompt)

<!--
* STIMMUNG & TONFALL:
  - Kompetent und erklärend.
  - Fokus auf Einfachheit ("Keine Angst vor Tech-Begriffen, wir machen das greifbar!").

* VISUALS IM VIDEO:
  - Die drei Säulen sollten nacheinander mit einem "Slide-In" Effekt von links eingeblendet werden.
  - Neben den Säulen Symbole einblenden: Ein Globus für Multi-Channel, ein Organigramm-Icon für Organisationen und ein Roboter- oder KI-Gehirn-Symbol für Agentic Engineering.

* SPRECHZETTEL / TALKING POINTS:
  - Was ist der Price Provider Service eigentlich genau? Stellen Sie sich eine einzige, zentrale Quelle der Wahrheit für alle Ihre Preise vor.
  - Egal ob Onlineshop, Kasse im Laden oder mobile App – alle fragen denselben Service über eine standardisierte Schnittstelle (API) ab.
  - Das Schöne daran: Es ist Open-Source, hochperformant und läuft extrem stabil.
  - Für Sie als Analysten besonders spannend sind die drei tragenden Säulen: Multi-Channel, hierarchische Kundengruppen und die Vorbereitung auf KI-gestützte Entwicklung (Agentic Engineering). Gehen wir diese Punkte im Detail durch.
-->

---

## Die Herausforderung im B2B- & B2C-Retail

* **Monolithische Alt-Systeme** sind träge und unflexibel.
* **Kanalkonflikte**: Unterschiedliche Preise für denselben Artikel im B2B-Onlineshop, im B2C-Shop und in der Vermietung.
* **Hierarchische B2B-Rabatte**: Partnerfirmen, Tochtergesellschaften und Einkaufsorganisationen benötigen präzise, vererbte Sonderkonditionen.
* **Wartungsaufwand**: Klassische IT-Projekte für Preisänderungen oder Tabellenerweiterungen dauern Monate.

<!--
* STIMMUNG & TONFALL:
  - Mitfühlend, problembewusst, drängend.
  - Betonung des Schmerzpunkts der Business Analysten ("Das kennen Sie sicher aus Ihrem Alltag...").

* VISUALS IM VIDEO:
  - Ein geteilter Bildschirm: Links eine symbolische, verstaubte "Monolith-Datenbank" mit roten Warnzeichen für "Träge" und "Teuer".
  - Rechts die verwirrenden Pfade von Preislisten, die sich gegenseitig widersprechen.
  - Eine Animation von Uhren, die sich schnell drehen, um die verlorene Zeit bei IT-Projekten zu verdeutlichen.

* SPRECHZETTEL / TALKING POINTS:
  - Warum brauchen wir überhaupt eine neue Lösung? Weil herkömmliche ERP- und Shopsysteme an ihre Grenzen stoßen.
  - Denken Sie an die typischen Probleme: Ein B2B-Kunde sieht versehentlich den günstigeren B2C-Aktionspreis. Oder die Pflege von Sonderkonditionen für Einkaufsgemeinschaften wird zum Excel-Albtraum mit tausenden Zeilen.
  - Jedes Mal, wenn Sie als Business Analyst ein neues Preismodell einführen wollen – zum Beispiel ein Mietmodell –, sagt Ihnen die IT: "Das dauert ein halbes Jahr und kostet ein Vermögen."
  - Genau hier bricht Composable Commerce diese veralteten Strukturen auf.
-->

---

## Die Lösung: Composable Commerce

```
                +---------------------------------------+
                |        Dynamic Angular Frontend       |
                +---------------------------------------+
                                    | (Public / Admin API)
                                    v
                +---------------------------------------+
                |         Price Provider Service        |
                +---------------------------------------+
                   |                 |               |
                   v                 v               v
            [ Keycloak IDP ]   [ Postgres DB ]  [ AI-Agent Skills ]
```

* **Entkoppelte Architektur**: Frontend und Backend sprechen über standardisierte Schnittstellen.
* **Echtzeit-Preisfindung**: Millisekunden-Antwortzeiten durch datenbankseitige Filterung und optimierte Algorithmen.

<!--
* STIMMUNG & TONFALL:
  - Erleichtert, dynamisch, überzeugend.
  - "Hier kommt die Rettung!"-Mentalität.

* VISUALS IM VIDEO:
  - Das abgebildete Text-Diagramm wird animiert aufgebaut.
  - Erst erscheint das Frontend (die Benutzeroberfläche), dann der Price Provider Service in der Mitte als Vermittler, und schließlich die drei Stützen unten (Keycloak für Sicherheit, Postgres für Daten, AI-Skills für die Zukunft).
  - Ein Blitz-Icon zwischen den Boxen symbolisiert die "Echtzeit-Geschwindigkeit" (unter 10 Millisekunden Antwortzeit).

* SPRECHZETTEL / TALKING POINTS:
  - Unsere Lösung basiert auf dem Prinzip des "Composable Commerce". Das bedeutet: Jede Aufgabe wird von dem System erledigt, das es am besten kann.
  - Das Frontend – also das, was der Kunde sieht – ist völlig unabhängig vom Backend. Sie sprechen über sichere Schnittstellen (APIs) miteinander.
  - Der Price Provider Service liegt im Zentrum. Er holt sich die Benutzerdaten vom Identity Provider (Keycloak), sucht blitzschnell in der PostgreSQL-Datenbank und berechnet exakt den richtigen Preis.
  - Das Ganze passiert in Millisekunden, sodass der Kunde im Shop keinerlei Verzögerung spürt.
-->

---

## Szenario 1: Der B2B-Onlineshop (Globale Steuerung)

### Das Konzept der Channels und Länderkonsistenz
Jeder Verkaufskanal (z. B. `global-b2b-sales-channel`) bedient exakt definierte Länder über Länder-ISO-Codes (z. B. `DE`, `US`).

### Steuer- und Preiskalkulation in Echtzeit
* **FORCE_NET (B2B)**: Preise werden im Checkout und Produktkatalog ohne MwSt. (Netto) dargestellt. Bei Bedarf berechnet das System die Steuer im Hintergrund.
* **FORCE_GROSS (B2C)**: Endkunden erhalten immer Bruttobeträge inklusive lokaler Steuersätze.
* **Mengenstaffeln (Tiered Pricing)**: Automatische Bestimmung des besten Preises ab Mindestmenge (z. B. 1 Stück = 10 €, ab 10 Stück = 8.50 €).

<!--
* STIMMUNG & TONFALL:
  - Strukturiert, analytisch, hochpräzise.
  - Fokus auf die geschäftlichen Vorteile für globale Händler.

* VISUALS IM VIDEO:
  - Eine Weltkarte, auf der die Länder Deutschland (DE) und USA (US) aufleuchten.
  - Zwei parallele Spalten: Eine für "B2B Onlineshop (Netto)" und eine für "B2C Shop (Brutto)".
  - Eine grafische Visualisierung einer Preisstaffel: Ein virtueller Einkaufskorb füllt sich, und je mehr Artikel hineingelegt werden, desto weiter sinkt der Stückpreis.

* SPRECHZETTEL / TALKING POINTS:
  - Schauen wir uns das erste konkrete Geschäftsszenario an: Ein globaler B2B-Onlineshop.
  - Hier müssen wir länderspezifische Regeln einhalten. Ein Verkaufskanal darf nur in zugelassenen Ländern verkaufen. Der Price Provider stellt diese Konsistenz automatisch sicher.
  - Ein riesiger Vorteil ist die flexible Steuerdarstellung: Für B2B stellen wir den Kanal einfach auf "FORCE_NET". Der Geschäftskunde sieht saubere Nettopreise, während das System im Hintergrund die Steuerklassen für den Export berechnet.
  - Für Ihren B2C-Kanal schalten Sie einfach auf "FORCE_GROSS" um – schon sieht der Endverbraucher Bruttopreise inklusive der korrekten Mehrwertsteuer.
  - Und natürlich sind Mengenstaffeln voll integriert. Erreicht der Warenkorb eine Mindestmenge, springt der Preis vollautomatisch auf die nächste Rabattstufe.
-->

---

## Szenario 1 in der Praxis

### B2B-Preisanfrage für ein Produkt:

```http
GET /public/api/global-b2b-sales-channel/DE/pricerows/SALES_PRICE/of/PROD-01
    ?quantity=15&unit=piece&currency=EUR&$expand=$info.taxation
```

### Die Antwort des Services (Auszug):
```json
{
  "pricedResourceId": "PROD-01",
  "priceValue": 89.99,
  "minQuantity": 10.00,
  "unitRef": "piece",
  "currencyRef": "EUR",
  "taxIncluded": false,
  "$info": {
    "taxation": {
      "taxValue": 17.09,
      "taxRate": 0.19,
      "taxIncludedInfo": "calculated (net representation)"
    }
  }
}
```

<!--
* STIMMUNG & TONFALL:
  - Souverän, technisch fundiert aber verständlich erklärt.
  - Betonung der Einfachheit und strukturierten Antwort.

* VISUALS IM VIDEO:
  - Das HTTP-Request-Beispiel links einblenden.
  - Das JSON-Antwort-Beispiel rechts einblenden.
  - Wichtige Zeilen farbig hervorheben: "priceValue: 89.99", "taxIncluded: false" und die berechnete Steuer "taxValue: 17.09".

* SPRECHZETTEL / TALKING POINTS:
  - Keine Angst vor dem Code auf dieser Folie! Dies ist das, was unter der Haube passiert – und es ist wunderschön strukturiert.
  - Das Frontend sendet eine einfache Anfrage: "Gib mir den Verkaufspreis für das Produkt PROD-01 im globalen B2B-Kanal für Deutschland, Menge 15 Stück, in Euro."
  - Der Service antwortet sofort im standardisierten JSON-Format.
  - Sehen Sie hier: Obwohl das Produkt eigentlich netto gepflegt ist, liefert uns der Parameter "$expand=$info.taxation" sofort alle Steuerdetails mit: 19 % Steuersatz, was bei 15 Stück einer Steuer von 17,09 Euro entspricht.
  - Das bedeutet: Ihr Frontend-Entwickler muss keine komplizierte Steuerlogik mehr programmieren. Er fragt einfach den Price Provider, und die Daten kommen perfekt formatiert zurück.
-->

---

## Szenario 2: B2B-Werkzeugvermietung (Komplexe Preismodelle)

*Bei der Vermietung von Profi-Geräten (z. B. Bohrhammer, Sägen, Mäher) reichen klassische Stückpreise nicht aus. Hier kombiniert der Price Provider zwei Preistypen:*

### 1. RENTAL_BASE_PRICE (Einmalige Bereitstellungsgebühr)
* Wird pro gemietetem Stück berechnet.
* Deckt Reinigung, Inspektion und Logistik ab.

### 2. RENTAL_DAILY_RATE (Laufende Tagesgebühr)
* Basiert auf der Zeiteinheit Tag (`d`).
* Gestaffelte Tagesgebühren bei längeren Mietzeiten (z. B. ab 7 Tagen günstiger, ab 14 Tagen Sparpreis).

<!--
* STIMMUNG & TONFALL:
  - Begeistert, lösungsorientiert.
  - Ein "Aha!"-Erlebnis vermitteln.

* VISUALS IM VIDEO:
  - Bilder von professionellen Werkzeugen (einem schweren gelb-schwarzen Bohrhammer und einer Kreissäge).
  - Ein grafisches Additionszeichen: [ Bereitstellungsgebühr ] + [ Tagesgebühr x Tage ] = [ Mietpreis ].
  - Hervorheben der Zeiteinheit "d" (für Tage) im Kontrast zu klassischen Stück-Einheiten.

* SPRECHZETTEL / TALKING POINTS:
  - Nun zu unserem zweiten Highlight-Szenario: Der Vermietung von Profi-Werkzeugen. Das ist ein extrem spannendes und lukratives B2B-Geschäftsfeld!
  - Ein Handwerker möchte eine schwere Bohrmaschine mieten. Wie berechnen wir das?
  - Typischerweise haben wir hier zwei Komponenten. Erstens: Den RENTAL_BASE_PRICE – eine einmalige Gebühr für die Bereitstellung, Wartung und Reinigung der Maschine.
  - Zweitens: Den RENTAL_DAILY_RATE – die tägliche Nutzungsgebühr. Und hier wird es clever: Je länger die Maschine gemietet wird, desto günstiger soll der Tagessatz werden.
  - Der Price Provider Service unterstützt diese komplexen Anforderungen nativ "out-of-the-box". Schauen wir uns an, wie das in der Praxis mit echten Daten aussieht.
-->

---

## Werkzeugvermietung: Live-Datenbeispiel `RENT-DRILL`

### Ein Standard-Bauunternehmer mietet 1 Profi-Bohrmaschine für 10 Tage:

* **Bereitstellungsgebühr (`RENTAL_BASE_PRICE`)**:
  * 1 Stück = **45.00 EUR** (Mengenstaffel 1)
* **Tagesmiete (`RENTAL_DAILY_RATE`)**:
  * 10 Tage = 10 × **18.00 EUR** (Staffelpreis ab 7 Tagen greift automatisch statt des Standardtarifs von 22.50 EUR!)
* **Gesamtpreis-Indikation**:
  * 45.00 EUR + 180.00 EUR = **225.00 EUR**

*Der Service wählt dank ausgeklügelter Priorisierungslogik vollautomatisch den optimalen Staffelpreis basierend auf der Menge und Mietdauer aus.*

<!--
* STIMMUNG & TONFALL:
  - Rechnerisch klar, stolz, demonstrierend.
  - Fokus auf die automatische Intelligenz des Systems.

* VISUALS IM VIDEO:
  - Eine schrittweise Rechnung, die sich aufbaut.
  - Eine Zeitleiste von Tag 1 bis Tag 10. Die Tage 1 bis 6 werden mit dem Standard-Tagespreis markiert, aber sobald Tag 7 erreicht wird, schaltet die Farbe um und zeigt den reduzierten Staffelpreis für alle 10 Tage an.
  - Ein dickes grünes Häkchen neben dem Gesamtpreis von 225.00 EUR.

* SPRECHZETTEL / TALKING POINTS:
  - Nehmen wir das reale Beispiel aus unserer Datenbank: Das Produkt `RENT-DRILL`, ein robuster Bohrhammer.
  - Ein Bauunternehmer mietet ein Gerät für 10 Tage.
  - Das System schlägt zuerst die Bereitstellungsgebühr nach: Für ein Gerät beträgt diese 45 Euro.
  - Nun berechnet das System die Tagesmiete für 10 Tage. Der normale Tagessatz liegt eigentlich bei 22,50 Euro. Aber da 10 Tage gemietet werden und wir eine Staffel für Buchungen ab 7 Tagen hinterlegt haben, greift automatisch der ermäßigte Tarif von 18,00 Euro pro Tag!
  - 10 Tage mal 18 Euro ergibt 180 Euro. Zusammen mit den 45 Euro Bereitstellung bezahlt der Kunde genau 225 Euro.
  - Das Geniale daran: Sie müssen diese Logik nicht im Onlineshop programmieren. Sie hinterlegen einfach die Preisreihen in der Datenbank des Price Providers, und die API liefert dem Kunden sofort das absolut korrekte, günstigste Angebot.
-->

---

## Szenario 2: Personalisierte B2B-Konditionen

*Im B2B-Sektor entscheidet die Firmenzugehörigkeit über den Preis. Der Service löst dies über hierarchische Keycloak-Organisationen:*

```
                /organizations/ (Einkaufsgemeinschaft)
                     |
         +-----------+-----------+
         |                       |
   ORG-RENTAL-BUILDER-PRO   ORG-RENTAL-GREEN-LAND
   (Hoch- & Tiefbau)        (Garten- & Landschaftsbau)
```

* **Individuelle Tarife**: `ORG-RENTAL-BUILDER-PRO` erhält exklusive Rabatte auf schwere Maschinen. `ORG-RENTAL-GREEN-LAND` zahlt weniger für Rasenmäher.
* **Hierarchische Vererbung**: Sonderpreise auf der übergeordneten Ebene vererben sich automatisch nach unten, sofern sie nicht spezifisch überschrieben werden.

<!--
* STIMMUNG & TONFALL:
  - Strategisch, strukturiert, visionär.
  - Verdeutlichung des massiven Vorteils bei Großkunden-Verhandlungen.

* VISUALS IM VIDEO:
  - Das Organisations-Baumdiagramm wird elegant animiert.
  - Linien leuchten auf, wenn ein Preis von der "Einkaufsgemeinschaft" nach unten zu den Tochtergesellschaften vererbt wird.
  - Symbole für die Branchen: Ein Bauhelm für Builder-Pro und ein Blatt/Baum für Green-Land.

* SPRECHZETTEL / TALKING POINTS:
  - Jetzt gehen wir noch einen Schritt weiter. Im B2B-Bereich hat fast jeder Großkunde individuelle Verträge.
  - Der Price Provider nutzt hierfür ein hierarchisches Organisationsmodell, das direkt an Ihre Benutzerverwaltung gekoppelt ist.
  - Sehen Sie sich dieses Beispiel an: Wir haben eine übergeordnete Einkaufsgemeinschaft. Darunter hängen zwei spezialisierte B2B-Kunden: "Builder-Pro" für den Hochbau und "Green-Land" für den Landschaftsbau.
  - Builder-Pro benötigt schwere Bohrhämmer und bekommt dort exzellente Sonderkonditionen. Green-Land dagegen mietet hauptsächlich Großflächenmäher und erhält dort Spezialpreise.
  - Loggt sich nun ein Mitarbeiter von "Builder-Pro" im Webshop ein, erkennt das System über sein Benutzer-Token (JWT) sofort die Zugehörigkeit und liefert ihm exakt seine vereinbarten B2B-Sonderkonditionen – vollautomatisch!
  - Wenn Sie globale Rabatte auf der obersten Ebene der Einkaufsgemeinschaft eintragen, vererben sich diese automatisch an alle Tochterfirmen. Das spart hunderte Stunden manueller Datenpflege!
-->

---

## Datensicherheit & Mandantenfähigkeit

*Wie verhindern wir, dass B2B-Kunden sensible Einkaufspreise sehen oder Währungshändler falsche Daten ändern?*

### Permission Selectors (Feingranulare Zugriffsregeln)
Administratoren können Berechtigungen mittels einfacher Ausdrücke einschränken:

* **Sperrung sensibler Einkaufspreise**:
  `priceprovider.public:PriceRow[NOT (priceType=='PURCHASE_PRICE' OR priceType=='MATERIAL_COST')]:read`
* **Währungsgebundene Schreibrechte für Mitarbeiter**:
  `priceprovider.admin:PriceRow[currencyRef=='EUR']:write`

*Keine Hardcodierung in der Software nötig – alles wird dynamisch zur Laufzeit über Keycloak-Rollen gesteuert!*

<!--
* STIMMUNG & TONFALL:
  - Sicherheitsbewusst, vertrauensvoll, beruhigend.
  - Fokus auf Compliance und einfache Administration.

* VISUALS IM VIDEO:
  - Ein virtuelles Schild mit einem Schloss-Symbol, das sich schließt.
  - Ein Vorher-Nachher-Szenario: Ein normaler Mitarbeiter sieht nur EUR-Preise, während ein globaler Admin alle Währungen bearbeiten kann.
  - Einblendung der Ausdrücke in einer lesbaren, großen Schrift, um zu zeigen, wie verständlich diese Regeln aufgebaut sind.

* SPRECHZETTEL / TALKING POINTS:
  - Ein extrem wichtiges Thema im B2B-Retail ist die Datensicherheit und Mandantenfähigkeit.
  - Sie wollen unter keinen Umständen, dass ein normaler Kunde Ihre internen Einkaufspreise oder Materialkosten ausliest. Oder dass ein Sachbearbeiter aus Versehen Preise in US-Dollar ändert, obwohl er nur für den Euro-Raum zuständig ist.
  - Der Price Provider löst dies genial über sogenannte "Permission Selectors".
  - Das sind feingranulare Zugriffsregeln, die Sie als Administrator direkt in der Benutzeroberfläche definieren können.
  - Schauen Sie sich die Beispiele an: Mit nur einer einzigen Zeile sperren wir den Zugriff auf alle Einkaufs- und Materialpreise für die öffentliche API.
  - Oder wir erlauben einem Euro-Preismanager ausschließlich das Schreiben von Preisen, bei denen die Währung exakt "EUR" entspricht.
  - Sie müssen dafür keine einzige Zeile Code ändern oder ein IT-Ticket eröffnen. Das erhöht Ihre Sicherheit und Flexibilität enorm!
-->

---

## Agentic Engineering: Die Zukunft der Software-Entwicklung

### KI-Agenten erweitern das System selbstständig
* Der Service ist mit **KI-Agent-Skills** ausgestattet.
* Entwickler und BAs können neue Features, Datenfelder oder Validierungsregeln einfach via **Prompt** beschreiben.

### Code-Generierung im Handumdrehen (CDF)
* **Class Definition Format (CDF)**: Das Datenmodell wird in deklarativen JSON-Dateien beschrieben.
* Das Gradle-Build-Plugin generiert daraus performanten, fehlerfreien Java-Code und JPA-Datenbankstrukturen.
* **Vorteil**: Keine Tippfehler, automatische Migrationen, maximale Entwicklungsgeschwindigkeit.

<!--
* STIMMUNG & TONFALL:
  - Innovativ, zukunftsweisend, fasziniert.
  - "Das ist die Zukunft, und wir sind jetzt schon bereit dafür!"-Haltung.

* VISUALS IM VIDEO:
  - Ein Chat-Fenster, in dem ein Benutzer einen Prompt eingibt: "Füge dem PriceRow-Modell ein Feld für 'Saison-Rabatt' hinzu."
  - Eine Animation, wie aus dieser Texteingabe blitzschnell eine JSON-Datei (CDF) entsteht und sich im Hintergrund fehlerfreier Java-Code generiert.
  - Ein glänzendes "Agentic Engineering Ready"-Badge.

* SPRECHZETTEL / TALKING POINTS:
  - Kommen wir zu einem echten technologischen Highlight: Agentic Engineering.
  - Wir alle wissen, wie mühsam es ist, wenn neue Geschäftsmodelle Anpassungen am Datenmodell erfordern. Normalerweise schreiben Entwickler tagelang manuellen "Boilerplate-Code" – also Standard-Verbindungsdaten, Getters und Setters.
  - Nicht bei uns! Der Price Provider nutzt das "Class Definition Format", kurz CDF.
  - Alle unsere Datenmodelle sind in einfachen JSON-Dateien beschrieben. Unser intelligentes Build-Plugin generiert daraus vollautomatisch den fertigen Java-Code für die Datenbank.
  - Das Beste daran: Unsere vordefinierten "KI-Agent-Skills" ermöglichen es KI-Assistenten, diese JSON-Modelle direkt zu verstehen und zu erweitern.
  - Sie beschreiben das neue Feature als Prompt, die KI generiert das CDF-Datenmodell, und das System baut sich fehlerfrei selbst auf. Das ist die absolute Zukunft der Software-Entwicklung!
-->

---

## Die Cloud-Plattform der Zukunft (Technical Architecture)

```
       +-------------------------------------------------------------+
       |                  Angular Dynamic UI App                     |
       |  (Generiert Formulare zur Laufzeit über das $meta-API)      |
       +-------------------------------------------------------------+
                                      |
                                      v [HTTPS / REST-API]
       +-------------------------------------------------------------+
       |             Price Provider Spring Boot Service              |
       |  (Zentrale Business Logik, RBAC & Permission Evaluation)     |
       +-------------------------------------------------------------+
               |                       |                      |
               v                       v                      v
     [ Keycloak OIDC ]         [ PostgreSQL / DB ]    [ CDF Build-Engine ]
  (Sicheres Identity Mgmt,    (Schnelle, rekursive    (Generiert JPA-Code,
   Orgs & Token Extraction)     CTE-Preisfindung)      kein Boilerplate!)
```

<!--
* STIMMUNG & TONFALL:
  - Professionell, strukturiert, überzeugend.
  - Zeigt die technologische Reife und das perfekte Zusammenspiel der Komponenten.

* VISUALS IM VIDEO:
  - Ein 3D-Schichtenmodell der Architektur.
  - Die Daten fließen von oben (Angular Frontend) nach unten (Spring Boot Backend) und verzweigen sich in die drei Teilsysteme.
  - Ein kurzes optisches Highlight auf der PostgreSQL-Datenbank für "rekursive CTE-Preisfindung" – das zeigt Tiefgang und Performance-Stärke.

* SPRECHZETTEL / TALKING POINTS:
  - Für die Technik-Interessierten unter Ihnen werfen wir einen kurzen Blick auf das Big Picture der Cloud-Architektur.
  - Ganz oben steht unsere Angular-App. Sie ist extrem dynamisch. Warum? Weil sie über unsere "$meta-API" die Struktur der Datenfelder abfragt und Formulare völlig automatisch zur Laufzeit generiert!
  - Darunter liegt der Price Provider Spring Boot Service – hochoptimiert für Java 25. Er kümmert sich um die gesamte Business-Logik und prüft die Sicherheitsregeln.
  - Er arbeitet Hand in Hand mit drei Bausteinen: Keycloak sorgt für das sichere Identity Management und liefert uns die B2B-Organisationen.
  - PostgreSQL speichert die Daten und berechnet hierarchische Preise rasend schnell über rekursive Abfragen.
  - Und die CDF Build-Engine sorgt dafür, dass Erweiterungen von Partnern oder KI-Agenten nahtlos einfließen. Eine absolut runde, moderne Cloud-Plattform!
-->

---

## Warum Business Analysten den Price Provider lieben:

1. **Unbegrenzte Flexibilität**: Neue Preismodelle (wie Werkzeugvermietung) lassen sich ohne Code-Anpassungen konfigurieren.
2. **Schnelle Time-to-Market**: Änderungen am Datenmodell werden über CDF-JSON-Dateien in Minuten umgesetzt.
3. **Zukunftssicher**: Nahtlose Integration in KI-gestützte Workflows (Agentic Engineering).
4. **Sicherheit ab Stunde Null**: Robuste, feingranulare Sicherheit durch Permission Selectors und Keycloak-Integration.

<!--
* STIMMUNG & TONFALL:
  - Zusammenfassend, inspirierend, motivierend.
  - Direktes Ansprechen des Zuschauers ("Das ist Ihr Werkzeug für den Erfolg!").

* VISUALS IM VIDEO:
  - Eine Liste, bei der die 4 Punkte nacheinander mit animierten Häkchen abgehakt werden.
  - Eine glückliche Person (z. B. ein Business Analyst vor einem Laptop), die erfolgreich ein neues Preismodell freischaltet.

* SPRECHZETTEL / TALKING POINTS:
  - Fassen wir noch einmal zusammen: Warum ist der Price Provider Service Ihr bester Freund als Business Analyst?
  - Erstens: Sie haben unbegrenzte Flexibilität. Egal, ob Sie klassisch verkaufen oder neu in die Vermietung einsteigen wollen – die Preismodelle sind bereits eingebaut.
  - Zweitens: Die Time-to-Market sinkt dramatisch. Sie müssen nicht auf monatelange IT-Projekte warten, um neue Felder hinzuzufügen. Dank CDF und Code-Generierung geht das in Minuten.
  - Drittens: Sie sind bereit für die Zukunft der KI-Entwicklung.
  - Und viertens: Ihre Daten sind absolut sicher und DSGVO-konform geschützt, ohne dass komplexe Eigenentwicklungen nötig sind.
  - Kurz gesagt: Der Price Provider nimmt Ihnen die technischen Sorgen ab, damit Sie sich voll auf Ihre Pricing-Strategie konzentrieren können!
-->

---

## Vielen Dank für Ihre Aufmerksamkeit!

### Starten Sie jetzt Ihre Composable Pricing-Reise!

* **Dokumentation**: Erkunden Sie die Guides im `doc/`-Verzeichnis.
* **Demo-Anwendung**: Starten Sie das System lokal mit `docker-compose up`.
* **Fragen?** Ihr Entwicklungs-Team und die KI-Agenten stehen bereit!

<br>
<center>

**Composable Commerce. Flexibel. Schnell. Sicher.**

</center>

<!--
* STIMMUNG & TONFALL:
  - Herzlich, einladend, motivierender Call-to-Action.
  - Ausblenden mit einem Lächeln in der Stimme.

* VISUALS IM VIDEO:
  - Einblenden von nützlichen Links oder einem QR-Code zur Dokumentation.
  - Schönes Outro-Logo von "Commercestack Solutions" oder des Projekts.
  - Musik wird langsam wieder lauter und blendet sanft aus.

* SPRECHZETTEL / TALKING POINTS:
  - Damit sind wir am Ende unseres Trainingsvideos angelangt.
  - Vielen Dank für Ihre Aufmerksamkeit!
  - Die gesamte Dokumentation sowie alle technischen Guides finden Sie direkt im Repository im Ordner `doc/`.
  - Probieren Sie es am besten selbst aus: Starten Sie die Demo-Anwendung mit einem einfachen `docker-compose up` und erleben Sie das Zusammenspiel von Angular-Frontend und Price Provider live auf Ihrem Rechner.
  - Bei Fragen stehen Ihnen Ihr Entwicklungs-Team und unsere KI-Agenten jederzeit zur Seite.
  - Viel Erfolg bei der Gestaltung Ihrer neuen, flexiblen Pricing-Welt! Bis zum nächsten Mal!
-->
