# Kotlin integration and ruling tests

Scanner integration and ruling corpora check packaged behavior beyond individual rule samples.
Covers: `its/plugin/`, `its/ruling/`, `its/sq-integration/`, `its/sources/`.

First initialize `git submodule update --init its/sources`.

| Suite | Command | Assertions |
| --- | --- | --- |
| Ruling (SIT) | `./gradlew :its:ruling:integrationTest --info --console=plain --no-daemon` | Scanner issues compared to per-project golden JSON; no server required. |
| Plugin (SIT) | `./gradlew :its:plugin:integrationTest --info --console=plain --no-daemon` | Scanner/plugin behavior. |
| SQ integration | `./gradlew :its:sq-integration:integrationTest` | Server-backed and language-server corpus behavior. |

Standard ruling actual files appear in `its/ruling/build/reports/ruling/<projectKey>/` even when the test fails; expectations live in `its/ruling/src/test/resources/expected/<projectKey>/`. The Kotlin compiler corpus runs only with `KOTLIN_COMPILER_IT_ENABLED=true`. The language-server corpus writes actual JSON under `its/sq-integration/build/tmp/actual/kotlin/kotlin-language-server/` and compares it with `its/sq-integration/src/integrationTest/resources/expected/kotlin/kotlin-language-server/`.

Use `-DreportAll=true` when inspecting all actual issues. See [README.md](../README.md) for additional server settings and exact golden-file steps. Do not update expected files without inspecting the changed issues.
