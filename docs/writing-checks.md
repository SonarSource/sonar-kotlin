# Writing Kotlin checks

Use the rule stub generator and the matching semantic test variant to keep registration and metadata consistent.
Covers: `sonar-kotlin-api/src/main/java/org/sonarsource/kotlin/api/checks/`, `sonar-kotlin-checks/`, `kotlin-checks-test-sources/`, `sonar-kotlin-plugin/`.

## Rule shape
`AbstractCheck` visits PSI nodes and reports via `KotlinFileContext`. For calls, `CallAbstractCheck` uses `FunMatcher` to select functions and `visitFunctionCall` for the rule body. Any K2 symbol/type operation requires `withKaSession`; see [architecture](architecture.md).

Run `./gradlew setupRuleStubs -Prule=S42 -PclassName=AnswersEverythingCheck` for Kotlin rules or `setupGradleRuleStubs` for Gradle DSL rules. These tasks create check, sample, test, `KotlinCheckList` registration and rule metadata. Rule metadata can also be updated with `./gradlew :sonar-kotlin-plugin:ruleApiGenerateRuleKotlin -Prule=S42` or `:sonar-kotlin-plugin:ruleApiUpdateKotlin`; generated JSON/HTML should not be hand-edited.

## Test shape
| Base | Use |
| --- | --- |
| `CheckTest` | Compilable sample with semantic information. |
| `CheckTestWithNoSemantics` | Behavior when type resolution is absent. |
| `CheckTestNonCompiling` | Noncompiling input. |
| `CheckTestForAndroidOnly` | Android-only behavior and a non-Android sample. |

Keep each check's `*Sample.kt` in `kotlin-checks-test-sources/`; annotate issue lines with `// Noncompliant {{message}}` and assert compliant alternatives. `KotlinVerifier` compares reported issues with the annotations. See nearby tests and [testing](testing.md) before choosing a variant.
