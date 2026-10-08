# Testing Kotlin changes

Choose a rule, API, integration or ruling check based on the boundary changed.
Covers: `sonar-kotlin-checks/src/test/`, `kotlin-checks-test-sources/`, `sonar-kotlin-api/src/test/`, `its/`.

| Change | Command | Inspect |
| --- | --- | --- |
| One rule | `./gradlew :sonar-kotlin-checks:test --tests 'org.sonarsource.kotlin.checks.CollectionShouldBeImmutableCheckTest'` | `*Sample.kt` annotations and no-semantics variants. |
| Check module | `./gradlew :sonar-kotlin-checks:test` | Test report and `KotlinVerifier` assertions. |
| Full build | `./gradlew build dist` | Build and distribution packaging. |
| Formatting | `./gradlew spotlessCheck` | Shared formatting rules. |
| Ruling or scanner behavior | `./gradlew :its:ruling:integrationTest --info --console=plain --no-daemon` | [Integration guide](integration-tests.md). |

Version catalog changes may need `./gradlew --write-verification-metadata sha256 help` to refresh `gradle/verification-metadata.xml`; inspect added checksums and do not hand-prune existing entries. This task rewrites a committed resource.

For AST inspection, use the `printAst` task described in [utils-kotlin/README.md](../utils-kotlin/README.md) rather than guessing PSI node kinds.
