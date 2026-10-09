# Kotlin analyzer agent guide

SonarQube analyzer plugin for Kotlin: 140+ rules for Kotlin and Kotlin Gradle DSL (`.kts`), metrics, and import of Detekt, ktLint and AndroidLint reports. Public repository on the K2 Analysis API.

## Start here
- Setup: `git submodule update --init -- build-logic/common`; add `its/sources` before integration tests. Java 21 and all build setup are in [README.md](README.md).
- Pick the narrowest check: one rule `./gradlew :sonar-kotlin-checks:test --tests 'org.sonarsource.kotlin.checks.CollectionShouldBeImmutableCheckTest'`; full build `./gradlew build dist`; formatting `./gradlew spotlessCheck`. More commands in [testing](docs/testing.md).
- New rule: `./gradlew setupRuleStubs -Prule=S42 -PclassName=AnswersEverythingCheck` (`setupGradleRuleStubs` for `.kts`). The stubs write the check, test, sample, metadata resources and the registry entry (`KotlinCheckList.kt` / `KotlinGradleCheckList.kt`); review the diff.
- Metadata refresh: `./gradlew :sonar-kotlin-plugin:ruleApiUpdateKotlin`; generated resources are not hand-edited.

## Working agreements
- Checks extend `AbstractCheck` or `CallAbstractCheck`; wrap K2 semantic access in `withKaSession`. Test semantic and missing-semantics cases when applicable.
- Follow nearby Kotlin implementations and tests for imports, naming and layout; prefer immutable values where practical.
- Name rule tests `{ClassName}Test` and use descriptive backtick-named Kotlin test functions for additional cases.
- Use invented, nonfunctional examples; never put real credentials into docs, fixtures, logs or prompts. Everything committed to this repository is public.
- Version bumps: the build fails verification until `./gradlew --write-verification-metadata sha256 help` refreshes `gradle/verification-metadata.xml` (see [testing](docs/testing.md)).
- `compiler-docs.md` at the root describes K1-era compiler APIs; do not follow it for K2 code.
- **Keep docs current.** If a change alters behavior, a command, a config key or a test workflow described in `AGENTS.md`, `README.md` or `docs/`, update the matching page in the same PR. Docs describe only the current state: rewrite or delete statements that are no longer true, and add no history or "previously..." notes. Follow [the docs guidelines](docs/README.md#writing-and-maintaining-these-docs).

## Read on demand
- Before changing module boundaries or the analysis pipeline: [docs/architecture.md](docs/architecture.md).
- Before updating ruling expectations or running scanner/server-backed tests: [docs/integration-tests.md](docs/integration-tests.md).
- To inspect PSI node kinds: `./gradlew sonar-kotlin-api:printAst --args="dot path/to/File.kt /tmp/ast.dot"` (see [utils-kotlin/README.md](utils-kotlin/README.md)).

After changes, inspect the diff for generated files and secrets; run the narrowest meaningful checks.

## Always loaded
Imported so they are always in context; needed for nearly every change.
@docs/writing-checks.md
@docs/testing.md

<!-- sonar:begin:sonarqube-agentic-analysis-protocol -->
# Vortex analysis protocol

Vortex analysis is the final confirmation layer at the end of every turn in which you wrote to one or more files in the workspace (create, edit, patch, format — any tool call that changed file contents on disk).

**Trigger:** the moment before you send your final reply for the turn.

When the trigger fires, you **MUST** run Vortex analysis on the full set of files you modified this turn — **one command, not a per-file loop**.

**Preferred:** pass every path you modified this turn in a single invocation (repeat `--file` for each path). Use paths **relative to the project root** (e.g. `src/foo.ts`). For end-of-turn analysis, **always** pass `--depth DEEP` — even when you changed only one file — so cross-file findings are included.

```bash
sonar analyze agentic --depth DEEP --file <path/to/file1> --file <path/to/file2>
```

**Fallback:** when you cannot reliably list every modified path (e.g. you did not track them, or git state is the only source of truth), use the git change set with DEEP analysis:

```bash
sonar analyze agentic --depth DEEP
```

Per-edit hooks run faster STANDARD analysis. End-of-turn analysis must always use `--depth DEEP` (including a single `--file`). Use `--depth STANDARD` only for optional faster passes — never for the mandatory end-of-turn run.

Non-negotiable rules:

1. **Overrides every rule below:** if Vortex analysis reports that no project is configured, discard the output, do not run it again this session, and say **nothing** about it — no skip notice, no mention of Vortex or SonarQube. Reply as if this protocol did not exist. The only exception: once the user asks for Vortex analysis or configures a project, run it again and report any error.
2. Mid-turn edits do **not** need Vortex analysis — only the final on-disk state matters. Skipping Vortex analysis at end-of-turn is a protocol violation.
3. Run Vortex analysis **before** sending your final reply, marking the task done, or handing control back to the user.
4. If Vortex analysis reports issues on lines you touched in this turn, fix them, then re-run Vortex analysis on the same scope (change set or explicit file list). Repeat until clean (or only pre-existing findings on lines you did not touch remain). Pre-existing findings on untouched lines are out of scope — do not "fix" them unless the user asked.
5. If Vortex analysis is skipped for any other reason (e.g. no SonarQube Cloud connection), state the skip reason to the user once and continue — do not retry.
6. Do not suppress, summarize away, or omit Vortex analysis findings from your reply. Surface them verbatim.
<!-- sonar:end:sonarqube-agentic-analysis-protocol -->
