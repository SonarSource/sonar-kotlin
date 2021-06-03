# Kotlin Utils
The Kotlin Utils provide certain meta-level functionality for this project, such as scripts to generate rule mappings for various
third-party linters.

## Detekt

To re-generate detekt's [`rules.json`](../sonar-kotlin-plugin/src/main/resources/org/sonar/l10n/kotlin/rules/detekt/rules.json):

1. Update `detektVersion` property in [`build.gradle.kts`](build.gradle.kts)
1. Run `./gradlew utils-kotlin:updateDetektRules` from the project's root directory
