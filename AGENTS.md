# Kotlin analyzer agent guide

Use this guide to find the smallest relevant check and the detailed development references.

## Start here
- [README.md](README.md) covers build setup, ruling inputs and rule metadata. Initialize shared logic with `git submodule update --init -- build-logic/common`; initialize `its/sources` before integration tests.
- Java 21 is configured in the root `build.gradle.kts`; dependency versions are in `settings.gradle.kts`. Spotless adds license headers to Kotlin/Java sources, runs ktlint on `*.gradle.kts`, and applies separate whitespace rules to miscellaneous files.

## Choose the smallest relevant check
- One rule test: `./gradlew :sonar-kotlin-checks:test --tests 'org.sonarsource.kotlin.checks.CollectionShouldBeImmutableCheckTest'`; module: `./gradlew :sonar-kotlin-checks:test`; full build: `./gradlew build dist`.
- Formatting: `./gradlew spotlessCheck`; rule stubs: `./gradlew setupRuleStubs -Prule=S42 -PclassName=AnswersEverythingCheck` (or `setupGradleRuleStubs` for `.kts`). Metadata: `./gradlew :sonar-kotlin-plugin:ruleApiUpdateKotlin`. These tasks rewrite generated resources; review the diff.
- Ruling: `./gradlew :its:ruling:integrationTest --info --console=plain --no-daemon`; plugin: `./gradlew :its:plugin:integrationTest --info --console=plain --no-daemon`. See [integration tests](docs/integration-tests.md) for special corpora and server-backed cases.

## Working agreements
- Read implementation and adjacent tests first. Use the rule stub and metadata generators rather than hand-editing generated resources. Register Kotlin checks in `KotlinCheckList` and Gradle DSL checks in `KotlinGradleCheckList`; see [writing checks](docs/writing-checks.md) for their distinct sample and test locations.
- Checks extend `AbstractCheck` or `CallAbstractCheck`; wrap K2 semantic access in `withKaSession`. Test semantic and missing-semantics cases when applicable. See [writing checks](docs/writing-checks.md).
- Follow nearby Kotlin implementations and tests for imports, naming and layout; prefer immutable values where practical.
- Name rule tests `{ClassName}Test` and use descriptive backtick-named Kotlin test functions for additional cases. Follow the module's `CheckTest` and `KotlinVerifier` conventions.
- Use invented, nonfunctional examples; never put real credentials into docs, fixtures, logs or prompts. Everything committed to this repository is public.
- **Keep docs current.** If a change alters behavior, a command, a config key or a test workflow described in `AGENTS.md` or `docs/`, update the matching page in the same PR. Docs describe only the current state: rewrite or delete statements that are no longer true, and add no history or "previously…" notes. Follow [the docs guidelines](docs/README.md#writing-and-maintaining-these-docs).

## Reference map
- [Architecture](docs/architecture.md), [writing checks](docs/writing-checks.md), [testing](docs/testing.md), and [integration tests](docs/integration-tests.md).

After changes, inspect the diff for generated files and secrets; run the narrowest meaningful checks.

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
