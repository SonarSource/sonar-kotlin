# Kotlin Utils
The Kotlin Utils provide certain meta-level functionality for this project, such as scripts to generate rule mappings for various
third-party linters.

## Detekt

To re-generate detekt's [`rules.json`](../sonar-kotlin-plugin/src/main/resources/org/sonar/l10n/kotlin/rules/detekt/rules.json):

1. Update `detektVersion` property in [`build.gradle.kts`](build.gradle.kts)
1. Run `./gradlew utils-kotlin:updateDetektRules` from the project's root directory

## ktlint

To re-generate ktlint's [`rules.json`](../sonar-kotlin-plugin/src/main/resources/org/sonar/l10n/kotlin/rules/ktlint/rules.json):

1. Update `ktlintVersion` property in [`build.gradle.kts`](build.gradle.kts)
1. Run `./gradlew utils-kotlin:updateKtlintRules` from the project's root directory

## Android Lint

To re-generate android lint's [`rules.json`](../../sonar-kotlin-plugin/src/main/resources/org/sonar/l10n/android/rules/androidlint/rules.json):

1. Update android lint with command `$ANDROID_SDK_HOME/tools/bin/sdkmanager --install "cmdline-tools;latest"`
2. Check Android Lint version with command `$ANDROID_SDK_HOME/cmdline-tools/$version/bin/lint --version`
3. Export android lint help `$ANDROID_SDK_HOME/cmdline-tools/$version/bin/lint --show > utils-kotlin/src/main/resources/android-lint-help.txt`
4. Run `./gradlew utils-kotlin:updateAndroidLintRules` from the project's root directory
