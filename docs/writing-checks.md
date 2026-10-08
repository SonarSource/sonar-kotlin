# Writing Kotlin checks

Use the rule stub generator and the matching semantic test variant to keep registration and metadata consistent.
Covers: `sonar-kotlin-api/src/main/java/org/sonarsource/kotlin/api/checks/`, `sonar-kotlin-checks/`, `kotlin-checks-test-sources/`, `sonar-kotlin-gradle/`, `sonar-kotlin-plugin/`.

## Rule shape
`AbstractCheck` visits PSI nodes (`visitCallExpression`, `visitNamedFunction`, etc.) and reports with `kotlinFileContext.reportIssue(...)`. `CallAbstractCheck` declares `functionsToVisit` with `FunMatcher` (for example by qualifier/type, name, argument types, extension or suspend status) and implements `visitFunctionCall`. Access K2 symbols and types inside `withKaSession`; see [architecture](architecture.md).

| Rule | Stub command | Registry | Test and generated sample |
| --- | --- | --- | --- |
| Kotlin (`.kt`) | `./gradlew setupRuleStubs -Prule=S42 -PclassName=AnswersEverythingCheck` | `sonar-kotlin-plugin/src/main/java/org/sonarsource/kotlin/plugin/KotlinCheckList.kt` | `sonar-kotlin-checks/src/test/java/org/sonarsource/kotlin/checks/` and `kotlin-checks-test-sources/src/main/kotlin/checks/<CheckClassName>Sample.kt` |
| Gradle DSL (`.kts`) | `./gradlew setupGradleRuleStubs -Prule=S6626 -PclassName=TaskDefinitionsCheck` | `sonar-kotlin-gradle/src/main/java/org/sonarsource/kotlin/gradle/KotlinGradleCheckList.kt` | `sonar-kotlin-gradle/src/test/java/org/sonarsource/kotlin/gradle/checks/` and `sonar-kotlin-gradle/src/test/samples/non-compiling/<CheckClassName>Sample.kts` |

Both tasks create the check, test, sample and registry entry, then invoke `:sonar-kotlin-plugin:ruleApiGenerateRuleKotlin` for metadata. The rule JSON/HTML lives under `sonar-kotlin-plugin/src/main/resources/org/sonar/l10n/kotlin/rules/kotlin/` for both rule types. See [README.md](../README.md) for metadata download and update commands; inspect generated resources rather than hand-editing them.

## Test shape
| Base | Use |
| --- | --- |
| `CheckTest` | Ordinary `.kt` sample with semantics (also the name of the Gradle module's `.kts` test base). |
| `CheckTestWithNoSemantics` | `.kt` `*SampleNoSemantics.kt` with empty classpath and dependencies. |
| `CheckTestNonCompiling` / `DefaultCheckTestNonCompiling` | `.kt` `*SampleNonCompiling.kt` under `kotlin-checks-test-sources/src/main/files/non-compiling/checks/`. |
| `CheckTestForAndroidOnly` | Android sample and `*SampleNonAndroid.kt` with no issues outside Android. |

For ordinary Kotlin rules, keep the compiling `*Sample.kt` and optional no-semantics/non-Android variants in `kotlin-checks-test-sources/src/main/kotlin/checks/`. Gradle DSL tests use their module's `CheckTest` and `.kts` samples in the Gradle directory above; some tests exercise named `build.gradle.kts` or `settings.gradle.kts` inputs there. Annotate issue lines with `// Noncompliant {{message}}` and include compliant alternatives. `KotlinVerifier` checks reported issues against these annotations using the test classpath for semantic tests; the no-semantics variant passes an empty classpath and dependency list. See nearby tests and [testing](testing.md) before choosing a variant.
