# Kotlin analysis architecture

The plugin parses Kotlin PSI and dispatches checks within a bounded K2 Analysis API session.
Covers: `sonar-kotlin-api/`, `sonar-kotlin-checks/`, `sonar-kotlin-plugin/`, `sonar-kotlin-gradle/`, `sonar-kotlin-external-linters/`, `sonar-kotlin-metrics/`, `sonar-kotlin-surefire/`, `sonar-kotlin-test-api/`, `kotlin-checks-test-sources/`, `utils-kotlin/`.

```text
.kt or .kts → KotlinTree/PSI → KotlinFileVisitor.scan (kaSession)
                                      ↓
                               KtChecksVisitor → AbstractCheck → issues
```

| Module | Responsibility |
| --- | --- |
| `sonar-kotlin-api/` | K2 standalone session, `KotlinFileContext`, visitors, `FunMatcher` and `ApiExtensions` helpers. |
| `sonar-kotlin-checks/` | Kotlin rule implementations. |
| `sonar-kotlin-test-api/` and `kotlin-checks-test-sources/` | `KotlinVerifier` and ordinary Kotlin rule samples. |
| `sonar-kotlin-plugin/` | Plugin assembly, `KotlinRulesDefinition`, `KotlinCheckList` for `.kt` checks, and shared rule metadata. |
| `sonar-kotlin-gradle/` | Kotlin Gradle DSL checks, `KotlinGradleCheckList`, and `.kts` samples in `src/test/samples/non-compiling/`. |
| `sonar-kotlin-external-linters/` | Detekt, ktLint and AndroidLint report import. |
| `sonar-kotlin-metrics/` | Complexity, lines of code and other metrics. |
| `sonar-kotlin-surefire/` | JUnit/Surefire test report import. |
| `utils-kotlin/` | AST printer and external linter mapping generators. |

`KotlinTree` / `KotlinSyntaxStructure` parse the input with IntelliJ PSI and a K2 `StandaloneAnalysisAPISession`. `KotlinFileVisitor.scan` bounds the analysis session around the visit. `KtChecksVisitor` flattens the PSI tree and dispatches `KtElement` nodes to each registered `AbstractCheck` (`KtVisitor<Unit, KotlinFileContext>`) via `accept`. `KotlinFileContext` carries the `ktFile`, `kaSession`, `inputFileContext` for reporting, and `regexCache`. Access types and symbols through `withKaSession` inside that lifetime; do not retain `KaSession`-derived values beyond it. See [writing checks](writing-checks.md) for `CallAbstractCheck`, `FunMatcher` and separate check registries.

Dependency versions are centralized in `settings.gradle.kts`; `analyzerCommonsVersionStr` drives every analyzer-commons artifact. If changing them, use Gradle verification metadata generation described in [testing](testing.md); the build fails verification until it is run. Do not manually prune entries.
