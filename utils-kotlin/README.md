# Kotlin Utils
The Kotlin Utils provide certain meta-level functionality for this project, such as scripts to generate rule mappings for various
third-party linters.

## Generating rule mappings for external linters

### Detekt

To re-generate detekt's [`rules.json`](../sonar-kotlin-plugin/src/main/resources/org/sonar/l10n/kotlin/rules/detekt/rules.json):

1. Update `detektVersion` property in [`build.gradle.kts`](build.gradle.kts)
1. Run `./gradlew utils-kotlin:updateDetektRules` from the project's root directory

### ktlint

To re-generate ktlint's [`rules.json`](../sonar-kotlin-plugin/src/main/resources/org/sonar/l10n/kotlin/rules/ktlint/rules.json):

1. Update `ktlintVersion` property in [`build.gradle.kts`](build.gradle.kts)
1. Run `./gradlew utils-kotlin:updateKtlintRules` from the project's root directory

### Android Lint

To re-generate android lint's [`rules.json`](../../sonar-kotlin-plugin/src/main/resources/org/sonar/l10n/android/rules/androidlint/rules.json):

1. Update android lint with command `$ANDROID_SDK_HOME/tools/bin/sdkmanager --install "cmdline-tools;latest"`
2. Check Android Lint version with command `$ANDROID_SDK_HOME/cmdline-tools/$version/bin/lint --version`
3. Export android lint help `$ANDROID_SDK_HOME/cmdline-tools/$version/bin/lint --show > utils-kotlin/src/main/resources/android-lint-help.txt`
4. Run `./gradlew utils-kotlin:updateAndroidLintRules` from the project's root directory

## Printing ASTs to Dot

You can print the AST generated from a `*.kt` or `*.kts` file in DOT (Graphviz) format, either on `stdout` or to a file. To print the AST to
`stdout`:

1. Run `./gradlew utils-kotlin:printAst --args="<path-to-input-kotlin-file>"`

To print the AST into an output file, simply add the path of the output file as second command line argument:

1. Run `./gradlew utils-kotlin:printAst --args="<path-to-input-kotlin-file> <path-to-output-file>"`

So for instance, if you want to visualize the AST for the file `Example.kt` do the following (you'll need to install graphviz for this):

```
./gradlew utils-kotlin:printAst --args="Example.kt /tmp/ast.dot"
dot -Tpng /tmp/ast.dot -o /tmp/ast.png
```

Now open the file `/tmp/ast.png` with an image viewer of your choice. See the documentation of the `dot` tool to find various 
alternative output formats.
