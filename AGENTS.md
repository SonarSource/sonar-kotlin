# Kotlin analyzer agent guide

Use this guide to find the smallest relevant check and the detailed development references.

## Start here
- [README.md](README.md) covers build setup, ruling inputs and rule metadata. Initialize shared logic with `git submodule update --init -- build-logic/common`; initialize `its/sources` before integration tests.
- The shared Java conventions target **Java 21**. Gradle/Kotlin version catalogs are in `settings.gradle.kts`; Java formatting uses shared Spotless Eclipse and SonarSource import order.

## Choose the smallest relevant check
- One rule test: `./gradlew :sonar-kotlin-checks:test --tests 'org.sonarsource.kotlin.checks.CollectionShouldBeImmutableCheckTest'`; module: `./gradlew :sonar-kotlin-checks:test`; full build: `./gradlew build dist`.
- Formatting: `./gradlew spotlessCheck`; rule stubs: `./gradlew setupRuleStubs -Prule=S42 -PclassName=AnswersEverythingCheck` (or `setupGradleRuleStubs` for `.kts`). Metadata: `./gradlew :sonar-kotlin-plugin:ruleApiUpdateKotlin`. These tasks rewrite generated resources; review the diff.
- Ruling: `./gradlew :its:ruling:integrationTest --info --console=plain --no-daemon`; plugin: `./gradlew :its:plugin:integrationTest --info --console=plain --no-daemon`. See [integration tests](docs/integration-tests.md) for special corpora and server-backed cases.

## Working agreements
- Read implementation and adjacent tests first. Use the rule stub and metadata generators rather than hand-editing generated resources; register a check in `KotlinCheckList`.
- Checks extend `AbstractCheck` or `CallAbstractCheck`; wrap K2 semantic access in `withKaSession`. Test semantic and missing-semantics cases when applicable. See [writing checks](docs/writing-checks.md).
- Use explicit imports, immutable data where practical, clear `var` inference and suitable `final` fields. Keep short streams on one line, break long chains before each `.`, and avoid nested streams.
- Name tests `{ClassName}Test`, use statically imported AssertJ `assertThat`, behavior-oriented names such as `shouldShowExpectedBehaviorWhenMeetsCondition`, and a class-under-test variable named after the class.
- Use invented, nonfunctional examples; never put real credentials into docs, fixtures, logs or prompts. Everything committed to this repository is public.
- **Keep docs current.** If a change alters behavior, a command, a config key or a test workflow described in `AGENTS.md`, `docs/` or `private/docs/`, update the matching page in the same PR. Docs describe only the current state: rewrite or delete statements that are no longer true, and add no history or "previously…" notes. Follow [the docs guidelines](docs/README.md#writing-and-maintaining-these-docs).

## Reference map
- [Architecture](docs/architecture.md), [writing checks](docs/writing-checks.md), [testing](docs/testing.md), [integration tests](docs/integration-tests.md), and [agent analysis](docs/agent-analysis.md).

After changes, inspect the diff for generated files and secrets; run the narrowest meaningful checks.
