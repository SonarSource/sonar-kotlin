# Kotlin analysis architecture

The plugin parses Kotlin PSI and dispatches checks within a bounded K2 Analysis API session.
Covers: `sonar-kotlin-api/`, `sonar-kotlin-checks/`, `sonar-kotlin-plugin/`, `sonar-kotlin-gradle/`, `sonar-kotlin-external-linters/`.

```text
.kt or .kts → KotlinTree/PSI → KotlinFileVisitor.scan (kaSession)
                                      ↓
                               KtChecksVisitor → AbstractCheck → issues
```

| Module | Responsibility |
| --- | --- |
| `sonar-kotlin-api/` | K2 standalone session, `KotlinFileContext`, visitor and matcher APIs. |
| `sonar-kotlin-checks/` | Kotlin rule implementations. |
| `sonar-kotlin-test-api/` and `kotlin-checks-test-sources/` | `KotlinVerifier` and rule samples. |
| `sonar-kotlin-plugin/` | Plugin assembly, check registration and rule metadata. |
| `sonar-kotlin-gradle/` | Kotlin Gradle DSL checks. |
| `sonar-kotlin-external-linters/`, `sonar-kotlin-metrics/`, `sonar-kotlin-surefire/` | External report import, metrics and test reports. |

`KotlinFileVisitor.scan` creates and clears the analysis session around the visit. Access types and symbols through `withKaSession` inside that lifetime; avoid keeping `KaSession`-derived values beyond it. `KtChecksVisitor` dispatches PSI nodes to registered `AbstractCheck` visitors. The standalone K2 environment is set up by the API module.

Dependency versions are centralized in `settings.gradle.kts`. If changing them, use Gradle verification metadata generation described in [testing](testing.md); do not manually prune entries.
