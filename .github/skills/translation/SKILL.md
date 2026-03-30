---
name: translation
description: 'skill for creating and updating text and labels in the backend service and frontend'
---

# General Guidelines for Translation Tasks
- Always create professional translations (for native speakers) to ensure accuracy and cultural appropriateness.
- Source-of-truth language is english (en)
- Create complete translations for all keys in the source language (english) for the target language.
- “Complete” means:
    - No missing keys vs source language, No empty strings
    - validate ICU/message format placeholders match (e.g. {count} exists in all languages)

# Relevant Files and Folders
location of files with strings to translate:
- for frontend app check: app/src/assets/i18n/**
- for backend service check:
    - service/src/main/resources/initialize/** (initial demo or essential data)
    - MessageKeys.java‎ files - they contain the translation keys string published by the backend service, these keys should be reflected in the translation files of the frontend app

Please always check and update the files in these folders. Do not touch other files.

# Resources
- This technical guide [i18n-guide.md](../../../app/docs/i18n-guide.md) helps to setup new translation keys and files, and contains best practices for translation work.

# Translation Tasks
For the translation work, please follow the tasks below. Each task focuses on a specific set of languages. Ensure that you work on all languges.

## translation task 1
compare with the english (en) translation keys and add professional translations for new keys in the language - for the following languages:
German (de)
French (fr)
Czech (cs)
Danish (da)
Beyond that: clean up translation keys that do not exist in the english (en) translation keys

## translation task 2
compare with the english (en) translation keys and add professional translations for new keys in the language - for the following languages:
Spanish (es)
Estonian (et)
Finnish (fi)
Croatian (hr)
Beyond that: clean up translation keys that do not exist in the english (en) translation keys

## translation task 3
compare with the english (en) translation keys and add professional translations for new keys in the language - for the following languages:
Japanese (ja)
Lithuanian (lt)
Latvian (lv)
Dutch (nl)
Beyond that: clean up translation keys that do not exist in the english (en) translation keys

## translation task 4
compare with the english (en) translation keys and add professional translations for new keys in the language - for the following languages:
Norwegian (no)
Polish (pl)
Portuguese (pt)
Slovenian (sl)
Beyond that: clean up translation keys that do not exist in the english (en) translation keys

## translation task 5
compare with the english (en) translation keys and add professional translations for new keys in the language - for the following languages:
Swedish (sv)
Chinese (zh)
Beyond that: clean up translation keys that do not exist in the english (en) translation keys

