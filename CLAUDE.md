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

# SonarQube CAG MCP Tools - Usage Directive (MUST FOLLOW)

**Always use the Guide-and-Verify workflow** for code generation and modification.

## GUIDE Phase - Before Generating Code

**Before generating or editing code** you MUST:
1. Call `get_guidelines` for project context and coding standards
2. Locate existing code with `search_by_signature_patterns` or `search_by_body_patterns`
3. Read implementation with `get_source_code`

**When changing architecture or dependencies** you MUST:
- Check `get_current_architecture` and `get_intended_architecture`
- Analyze impact using:
  - `get_upstream_call_flow` / `get_downstream_call_flow` - trace method calls
  - `get_references` - find all usages
  - `get_classlike_structure_hierarchy` - check inheritance

## VERIFY Phase - After Generating Code

You must strictly follow this Analyze-Then-Commit workflow for every code modification. No code is considered complete until it has passed the following SonarQube validation loop:

1. Read Phase: After any modification or before commit, use the `Read` tool to load the current state of all relevant source files.
2. Analysis Phase: For every new or modified file, you must call `run_advanced_code_analysis` using:
    * `projectKey`: `SonarSource_sonar-kotlin`
    * `filePath`: The project-relative path to the file.
    * `fileContent`: The full, updated content of the file.
    * `branchName`: The active development branch.
    * `fileScope`: "MAIN" or "TEST" depending on the code type.
3. Evaluation & Remediation:
    * Rule Lookup: For every issue flagged, call `show_rule` with the specific rule key (e.g., `python:S1192`).
    * Mandatory Fixes: You are prohibited from committing code with **CRITICAL** or **HIGH** issues. You must implement fixes based on the rule's rationale and recommended guidance immediately.
4. Verification: After applying fixes, you must re-run the analysis to ensure the issues are resolved and no regressions were introduced.
