# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is **sonar-kotlin**, a SonarSource static code analyzer plugin for Kotlin. It integrates with SonarQube/SonarCloud to provide 130+ code quality and security rules for Kotlin projects. It also supports Kotlin Gradle DSL (`.kts`) analysis and imports from external linters (Detekt, ktLint, AndroidLint).

## Build Setup

Before first build, initialize the build-logic submodule:
```shell
git submodule update --init -- build-logic/common
```

## Dependency Version Bumps

Dependency versions are declared as version catalogs in `settings.gradle.kts` (e.g. the single `analyzerCommonsVersionStr` drives every `sonar-analyzer-commons` artifact). This repo enforces dependency verification, so after changing any version you must refresh the checksums or the build fails with a verification error:

```shell
./gradlew --write-verification-metadata sha256
```

This updates `gradle/verification-metadata.xml`. The task is additive — it appends entries for the new version but leaves the old ones in place (matching how prior bumps in this repo were done); don't hand-prune the superseded entries.

## Common Commands

```shell
# Build and run unit tests
./gradlew build dist

# Run all tests for a specific module
./gradlew :sonar-kotlin-checks:test

# Run a single test class
./gradlew :sonar-kotlin-checks:test --tests "org.sonarsource.kotlin.checks.CollectionShouldBeImmutableCheckTest"

# Integration and ruling tests both need the source projects submodule:
git submodule update --init its/sources

# Run ruling tests (uses SIT — no SonarQube server needed)
./gradlew :its:ruling:integrationTest --info --console=plain --no-daemon

# Run plugin tests
./gradlew :its:plugin:integrationTest --info --console=plain --no-daemon

# Update ruling golden files after changing a rule:
# The tests always write actual results to its/ruling/build/reports/ruling/ (even on failure).
# After running, copy the updated file(s) for the affected rule(s):
#   cp its/ruling/build/reports/ruling/<corpus>/kotlin-S<NNNN>.json \
#      its/ruling/src/integrationTest/resources/expected/kotlin/<corpus>/kotlin-S<NNNN>.json
```

## Implementing a New Rule

```shell
# Scaffold all boilerplate for a new rule (check class, test, test sample, metadata, registration)
./gradlew setupRuleStubs -Prule=S42 -PclassName=AnswersEverythingCheck

# For Kotlin Gradle DSL rules
./gradlew setupGradleRuleStubs -Prule=S6626 -PclassName=TaskDefinitionsCheck

# Download/refresh rule metadata from RSPEC repository
./gradlew :sonar-kotlin-plugin:ruleApiGenerateRuleKotlin -Prule=S42

# Update all rule metadata
./gradlew :sonar-kotlin-plugin:ruleApiUpdateKotlin
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

<!-- sonar:begin:sonarqube-agentic-analysis-protocol -->
# Vortex analysis protocol

Vortex analysis is the final confirmation layer at the end of every turn in which you wrote to one or more files in the workspace (create, edit, patch, format — any tool call that changed file contents on disk).

**Trigger:** the moment before you send your final reply for the turn.

When the trigger fires, you **MUST** run Vortex analysis on the full set of files you modified this turn — **one command, not a per-file loop**.

**Preferred:** pass every path you modified this turn in a single invocation (repeat `--file` for each path). Use paths **relative to the project root** (e.g. `src/foo.ts`). For end-of-turn analysis, **always** pass `--depth DEEP` — even when you changed only one file — so cross-file findings are included.

```bash
sonar analyze agentic --project SonarSource_sonar-kotlin --depth DEEP --file <path/to/file1> --file <path/to/file2>
```

**Fallback:** when you cannot reliably list every modified path (e.g. you did not track them, or git state is the only source of truth), use the git change set with DEEP analysis:

```bash
sonar analyze agentic --project SonarSource_sonar-kotlin --depth DEEP
```

Per-edit hooks run faster STANDARD analysis. End-of-turn analysis must always use `--depth DEEP` (including a single `--file`). Use `--depth STANDARD` only for optional faster passes — never for the mandatory end-of-turn run.

Non-negotiable rules:

1. Mid-turn edits do **not** need Vortex analysis — only the final on-disk state matters. Skipping Vortex analysis at end-of-turn is a protocol violation.
2. Run Vortex analysis **before** sending your final reply, marking the task done, or handing control back to the user.
3. If Vortex analysis reports issues on lines you touched in this turn, fix them, then re-run Vortex analysis on the same scope (change set or explicit file list). Repeat until clean (or only pre-existing findings on lines you did not touch remain). Pre-existing findings on untouched lines are out of scope — do not "fix" them unless the user asked.
4. If Vortex analysis is skipped (no SonarQube Cloud connection, or no project configured), state the skip reason to the user once and continue — do not retry.
5. Do not suppress, summarize away, or omit Vortex analysis findings from your reply. Surface them verbatim.
<!-- sonar:end:sonarqube-agentic-analysis-protocol -->
