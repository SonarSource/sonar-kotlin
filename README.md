Code Quality and Security for Kotlin
==========

[![Build Status](https://api.cirrus-ci.com/github/SonarSource/sonar-kotlin.svg?branch=master)](https://cirrus-ci.com/github/SonarSource/sonar-kotlin) [![Quality Gate Status](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.sonarsource.kotlin%3Akotlin&metric=alert_status)](https://next.sonarqube.com/sonarqube/dashboard?id=org.sonarsource.kotlin%3Akotlin) [![Coverage](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.sonarsource.kotlin%3Akotlin&metric=coverage)](https://next.sonarqube.com/sonarqube/dashboard?id=org.sonarsource.kotlin%3Akotlin)

This SonarSource project is a code analyzer for Kotlin projects to help developers write projects with [integrated code quality and security](https://www.sonarsource.com/solutions/mobile-developers/?utm_medium=referral&utm_source=github&utm_content=sonar-kotlin&utm_term=sonar-kotlin-readme).

Features
--------

* 50+ rules (including 10+ security rules using semantic)
* Metrics (cognitive complexity, cyclomatic complexity, number of lines etc.)
* Import of [test coverage reports](https://docs.sonarqube.org/display/PLUG/Code+Coverage+by+Unit+Tests+for+Java+Project)
* Import of [external linters](https://docs.sonarqube.org/latest/analysis/external-issues/): Detekt, ktLint, AndroidLint

Useful links
------------

* [Project homepage](https://redirect.sonarsource.com/plugins/kotlin.html)
* [Issue tracking](https://jira.sonarsource.com/browse/SONARKT/)
* [Available rules](https://rules.sonarsource.com/kotlin)
* [Community Forum](https://community.sonarsource.com/)

### Build

#### Setup

To configure build dependencies, run the following command:

```shell
git submodule update --init -- build-logic/common
```

To always get the latest version of the build logic during git operations, set the following configuration:

```shell
git config submodule.recurse true
```

For more information see [README.md](https://github.com/SonarSource/cloud-native-gradle-modules/blob/master/README.md) of cloud-native-gradle-modules.

#### Build and run Unit Tests:

    ./gradlew build dist

## Integration Tests

Integration and ruling tests require the source projects submodule:

    git submodule update --init its/sources

Run ruling tests (uses SIT — no SonarQube server needed):

    ./gradlew :its:ruling:integrationTest --info --console=plain --no-daemon

Run plugin tests:

    ./gradlew :its:plugin:integrationTest --info --console=plain --no-daemon

To run a single ruling test method, e.g.:

    ./gradlew :its:ruling:integrationTest --info --console=plain --no-daemon --tests "org.sonarsource.kotlin.its.KotlinRulingTest.test_kotlin_corda"

### Updating ruling golden files

The ruling tests diff actual scanner output against golden files. After changing a rule,
update the golden files for every corpus that exercises it.

#### Standard corpora (corda, okio, intellij-rust, …)

Golden files live under `its/ruling/src/integrationTest/resources/expected/`.
Actual results are always written to `its/ruling/build/reports/ruling/` even when tests fail.

1. Run the ruling tests:

       ./gradlew :its:ruling:integrationTest --info --console=plain --no-daemon

2. Copy the updated file(s) for the affected rule(s):

       cp its/ruling/build/reports/ruling/<corpus>/kotlin-S<NNNN>.json \
          its/ruling/src/integrationTest/resources/expected/kotlin/<corpus>/kotlin-S<NNNN>.json

3. Re-run to confirm they pass.

#### kotlin corpus (`test_kotlin_compiler`)

This corpus is skipped by default because it requires heavy Kotlin compiler sources.
Enable it with an environment variable:

    KOTLIN_COMPILER_IT_ENABLED=true ./gradlew :its:ruling:integrationTest --info --console=plain --no-daemon

Actual results land in the same `its/ruling/build/reports/ruling/kotlin/` directory.
Update golden files the same way as for the standard corpora.

#### kotlin-language-server corpus (`test_kotlin_language_server`)

This corpus is run by the `qa_sq_integration` CI job, not the `qa_ruling` job.
Golden files live under `its/sq-integration/src/integrationTest/resources/expected/kotlin/kotlin-language-server/`.
Actual results are written to `its/sq-integration/build/tmp/actual/kotlin/kotlin-language-server/` during the run.

1. Run the sq-integration tests:

       ./gradlew :its:sq-integration:integrationTest --info --console=plain --no-daemon -Dsonar.runtimeVersion=DEV

2. Copy the updated golden files:

       cp its/sq-integration/build/tmp/actual/kotlin/kotlin-language-server/kotlin-S<NNNN>.json \
          its/sq-integration/src/integrationTest/resources/expected/kotlin/kotlin-language-server/kotlin-S<NNNN>.json

3. Re-run to confirm they pass.

### Additional ruling parameters

* `-DreportAll=true` — dump all actual issues instead of only the differences
  (supported by `:its:ruling:integrationTest` and `:its:sq-integration:integrationTest`).

The orchestrator-based `:its:sq-integration` tests additionally support:

* `-Dsonar.runtimeVersion=<version>` — override the SonarQube version (default: `DEV`)
* `-DcleanProjects=true` — force a clean build of the analysed projects
* `-DkeepSonarqubeRunning=true` — leave the SonarQube instance running after the analysis
* `-Dsonar.rulingDebugPort=5005` — attach a debugger to the spawned scanner JVM

## Utilities and Developing

### Generating/downloading rule metadata

The Gradle task `ruleApiGenerateRuleKotlin` will download the rule metadata from the [RSPEC repository](https://github.com/SonarSource/rspec/).

For example, execute the following in the project root to fetch the metadata for rule `S42`:

    ./gradlew :sonar-kotlin-plugin:ruleApiGenerateRuleKotlin -Prule=S42

If fetching from a branch:

    ./gradlew :sonar-kotlin-plugin:ruleApiGenerateRuleKotlin -Prule=S4830 -Pbranch=a_branch

Alternatively, you can let the tool auto-detect the branch. If you do not provide a branch, it will look at the PRs
open in the RSPEC repository that contain the rule key in their name. If it finds any, you will be presented with a
choice of which branch to fetch the metadata from. Points to note about this feature:

* You can also add `-PautoSelectBranch` if you would like the script to automatically use the first branch it finds,
  if any, instead of prompting you for an interactive decision.
* You can specify `-Pbranch=master` to default to master.
* Usually, this feature should work as-is. However, it is possible to run into GitHub's rate limiting, which is lower
  for unauthenticated API requests. If you store a GitHub API token in the environment variable `GH_API_TOKEN`,
  it will be used for all requests to GitHub. Make sure you give the token sufficient rights to fetch details about and
  search pull requests in the RSPEC repository.

If you want to update all rules' metadata, you can use:

    ./gradlew :sonar-kotlin-plugin:ruleApiUpdateKotlin

### Implementing a new rule

The Gradle task `setupRuleStubs` will create the commonly required files for implementing a new rule, including usual boilerplate code. It
will also put the rule into the list of checks and call `ruleApiGenerateRuleKotlin` to download the rule's metadata.

To use this task, you need to know the rule key and a fitting name for the check class. For instance, if you want to implement the new
rule `S42` in the class `AnswersEverythingCheck`, you can call the following in the root of the project:

    ./gradlew setupRuleStubs -Prule=S42 -PclassName=AnswersEverythingCheck

To create stubs for Kotlin Gradle DSL rules instead of Kotlin rules, use `setupGradleRuleStubs`, as in: 

    ./gradlew setupGradleRuleStubs -Prule=S6626 -PclassName=TaskDefinitionsCheck

### Updating external linter rule mappings

See [this README in the utils](utils-kotlin/README.md).

### Visualizing ASTs

If you want a graphical output of ASTs, see [this README in the utils](utils-kotlin/README.md) for more info on how to convert an AST into a
DOT format.

# License

Copyright 2018-2025 SonarSource.

SonarQube analyzers released after November 29, 2024, including patch fixes for prior versions,
are published under the [Sonar Source-Available License Version 1 (SSALv1)](LICENSE.txt).

See individual files for details that specify the license applicable to each file.
Files subject to the SSALv1 will be noted in their headers.
