# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is **sonar-kotlin**, a SonarSource static code analyzer plugin for Kotlin. It integrates with SonarQube/SonarCloud to provide 130+ code quality and security rules for Kotlin projects. It also supports Kotlin Gradle DSL (`.kts`) analysis and imports from external linters (Detekt, ktLint, AndroidLint).

## Build Setup

Before first build, initialize the build-logic submodule:
```shell
git submodule update --init -- build-logic/common
```

## Common Commands

```shell
# Build and run unit tests
./gradlew build dist

# Run all tests for a specific module
./gradlew :sonar-kotlin-checks:test

# Run a single test class
./gradlew :sonar-kotlin-checks:test --tests "org.sonarsource.kotlin.checks.CollectionShouldBeImmutableCheckTest"

# Run integration tests (requires: git submodule update --init its/sources)
./gradlew build -Pits --info --console=plain --no-daemon

# Run ruling tests only
./gradlew build -Pruling --info --console=plain --no-daemon

# Run plugin tests only
./gradlew build -Pplugin --info --console=plain --no-daemon
```

## Implementing a New Rule

```shell
# Scaffold all boilerplate for a new rule (check class, test, test sample, metadata, registration)
./gradlew setupRuleStubs -Prule=S42 -PclassName=AnswersEverythingCheck

# For Kotlin Gradle DSL rules
./gradlew setupGradleRuleStubs -Prule=S6626 -PclassName=TaskDefinitionsCheck

# Download/refresh rule metadata from RSPEC repository
./gradlew generateRuleMetadata -Prule=S42

# Update all rule metadata
./gradlew updateRuleMetadata
```

## Architecture

### Module Structure

| Module | Purpose |
|--------|---------|
| `sonar-kotlin-api` | Core analysis framework: PSI/K2 parsing, check base classes, `FunMatcher`, `ApiExtensions`, visitor infrastructure |
| `sonar-kotlin-checks` | All Kotlin rule implementations (`*Check.kt` files) |
| `sonar-kotlin-test-api` | `KotlinVerifier` and test infrastructure for rule tests |
| `kotlin-checks-test-sources` | Kotlin sample files (`*Sample.kt`, `*SampleNoSemantics.kt`) used as rule test inputs |
| `sonar-kotlin-plugin` | Plugin assembly: `KotlinCheckList`, `KotlinRulesDefinition`, rule metadata (JSON/HTML in `src/main/resources`) |
| `sonar-kotlin-gradle` | Rules specific to Kotlin Gradle DSL (`.kts`) files |
| `sonar-kotlin-external-linters` | Import support for Detekt, ktLint, AndroidLint reports |
| `sonar-kotlin-metrics` | Metrics computation (complexity, lines of code, etc.) |
| `sonar-kotlin-surefire` | JUnit/Surefire test report import |
| `utils-kotlin` | Dev utilities: AST printer, external linter rule mapping generators |

### Analysis Pipeline

1. **Parsing**: `KotlinTree` / `KotlinSyntaxStructure` parse `.kt` files using the IntelliJ PSI + Kotlin Analysis API (K2 mode via `StandaloneAnalysisAPISession`).
2. **Session management**: `KotlinFileVisitor.scan()` wraps analysis in a `kaSession` block (K2 Analysis API session). Use `withKaSession { }` inside check visitors to access semantic information.
3. **Check dispatch**: `KtChecksVisitor` flattens the PSI tree and dispatches each node to all registered `AbstractCheck` visitors via `KtVisitor.accept()`.
4. **Checks**: Each check extends `AbstractCheck` (a `KtVisitor<Unit, KotlinFileContext>`) and overrides `visitXxx` methods for specific PSI node types.

### Key Abstractions

- **`AbstractCheck`**: Base class for all rules. Override `visitCallExpression`, `visitNamedFunction`, etc. Report issues via `kotlinFileContext.reportIssue(...)`.
- **`CallAbstractCheck`**: Convenience base for rules that trigger on specific function calls. Declare `functionsToVisit` using `FunMatcher` and override `visitFunctionCall`.
- **`FunMatcher` / `FunMatcherImpl`**: DSL for matching function calls by qualifier/type, name, argument types, extension status, suspend status, etc.
- **`KotlinFileContext`**: Passed to all visitor methods; provides `ktFile`, `kaSession` (semantic analysis), `inputFileContext` (for reporting), and `regexCache`.
- **`withKaSession { }`**: Must be called whenever accessing K2 Analysis API methods (type resolution, symbol lookup, etc.) inside a check.

### Rule Test Pattern

Each check has:
1. **Test class** in `sonar-kotlin-checks/src/test/java/org/sonarsource/kotlin/checks/`: extends one of:
   - `CheckTest` — standard test with semantics
   - `CheckTestWithNoSemantics` — tests behavior without type resolution
   - `CheckTestNonCompiling` — for code that doesn't compile
   - `CheckTestForAndroidOnly` — for checks that only apply in Android context; also verifies no issues on non-Android sample (`*SampleNonAndroid.kt`)
2. **Sample file** in `kotlin-checks-test-sources/src/main/kotlin/checks/`: named `<CheckClassName>Sample.kt` (and optionally `<CheckClassName>SampleNoSemantics.kt` or `<CheckClassName>SampleNonAndroid.kt`).

Issues in sample files are annotated with inline comments:
```kotlin
val x: MutableList<Int> // Noncompliant {{Make this collection immutable.}}
val y: List<Int> // Compliant
```

`KotlinVerifier` runs the check against the sample file and verifies the reported issues match the `Noncompliant` annotations. Tests with semantics compile the sample via the full classpath; tests with `emptyList()` classpath verify behavior without type resolution.

### Adding a Check to the Plugin

After creating a check class, it must be added to `KotlinCheckList.kt` in `sonar-kotlin-plugin` and registered in the appropriate rule metadata JSON/HTML under `sonar-kotlin-plugin/src/main/resources/org/sonar/l10n/kotlin/rules/kotlin/`. The `setupRuleStubs` task handles all of this automatically.

## AST Visualization

```shell
./gradlew sonar-kotlin-api:printAst --args="dot path/to/File.kt /tmp/ast.dot"
dot -Tpng /tmp/ast.dot -o /tmp/ast.png
```
