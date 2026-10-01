# Kotlin integration and ruling tests

Scanner integration and ruling corpora check packaged behavior beyond individual rule samples.
Covers: `its/plugin/`, `its/ruling/`, `its/sq-integration/`, `its/sources/`.

First initialize `git submodule update --init its/sources`.

| Suite | Command | Assertions |
| --- | --- | --- |
| Ruling (SIT) | `./gradlew :its:ruling:integrationTest --info --console=plain --no-daemon` | Scanner issues compared to per-project golden JSON; no server required. |
| Plugin (SIT) | `./gradlew :its:plugin:integrationTest --info --console=plain --no-daemon` | Scanner/plugin behavior. |
| SQ integration | `./gradlew :its:sq-integration:integrationTest` | Server-backed and language-server corpus behavior. |

Run a single ruling corpus with `--tests "org.sonarsource.kotlin.its.KotlinRulingTest.test_kotlin_corda"` appended to the ruling command.

## Updating ruling expectations

After changing a rule, inspect its actual JSON (written even when a test fails) and update each corpus it affects. For a standard corpus, copy the relevant rule file after reviewing the changed issues:

```shell
cp its/ruling/build/actual/<projectKey>/kotlin-S<NNNN>.json \
  its/ruling/src/test/resources/expected/<projectKey>/kotlin-S<NNNN>.json
```

The Kotlin compiler corpus (`test_kotlin_compiler`) is skipped by default. Run it with `KOTLIN_COMPILER_IT_ENABLED=true ./gradlew :its:ruling:integrationTest --info --console=plain --no-daemon`; its actual and expected rule JSON use the same ruling paths above (project key `kotlin-kotlin-project`).

The language-server corpus (`test_kotlin_language_server`) runs with `:its:sq-integration:integrationTest`. Review its actual file and update its separate expectation:

```shell
cp its/sq-integration/build/tmp/actual/kotlin/kotlin-language-server/kotlin-S<NNNN>.json \
  its/sq-integration/src/integrationTest/resources/expected/kotlin/kotlin-language-server/kotlin-S<NNNN>.json
```

Use `-DreportAll=true` to inspect all actual issues. See [README.md](../README.md) for additional server settings. Do not update expected files without inspecting the changed issues.
