Code Quality and Security for Kotlin
==========

[![Build Status](https://api.cirrus-ci.com/github/SonarSource/sonar-kotlin.svg?branch=master)](https://cirrus-ci.com/github/SonarSource/sonar-kotlin) [![Quality Gate Status](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.sonarsource.kotlin%3Akotlin&metric=alert_status)](https://next.sonarqube.com/sonarqube/dashboard?id=org.sonarsource.kotlin%3Akotlin) [![Coverage](https://next.sonarqube.com/sonarqube/api/project_badges/measure?project=org.sonarsource.kotlin%3Akotlin&metric=coverage)](https://next.sonarqube.com/sonarqube/dashboard?id=org.sonarsource.kotlin%3Akotlin)

This SonarSource project is a code analyzer for Kotlin projects.

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

Build and run Unit Tests:

    ./gradlew build

## Integration Tests

By default, Integration Tests (ITs) are skipped during the build. If you want to run them, you need first to retrieve the related projects
which are used as input:

    git submodule update --init its/sources

Then you need to build Ktor project:

    cd its/sources/kotlin/ktor

Follow the instructions to [build the project](https://github.com/ktorio/ktor/blob/main/CONTRIBUTING.md#building-the-project) and go back to the project root directory.

Then build and run the Integration Tests using the `its` property:

    ./gradlew build -Pits --info --console=plain --no-daemon

You can also build and run only Ruling Tests using the `ruling` property:

    ./gradlew build -Pruling --info --console=plain --no-daemon

You can also build and run only Plugin Tests using the `plugin` property:

    ./gradlew build -Pplugin --info --console=plain --no-daemon

To run e.g. the ruling tests in the IDE, create a new Run/Debug Configuration where you run the following:

    :its:ruling:test --info --console=plain -Pruling

You can also run single ruling tests, e.g.:

    :its:ruling:test --info --console=plain -Pruling --tests "org.sonarsource.slang.SlangRulingTest.test_kotlin_corda"

**Additional ruling parameters**

* By default, the SonarQube version used is LATEST_RELEASE, you can use the following property to set a different one:

      -Dsonar.runtimeVersion=9.7.1.62043

* By default, analyzed projects are built by gradle only if changed, but you can force a clean build by using:

      -DcleanProjects=true

* To keep SonarQube running at the end of the analysis:

       -DkeepSonarqubeRunning=true

* To see in SonarQube not only the issue differences but all the issues:

       -DkeepSonarqubeRunning=true -DreportAll=true

### Debugging ruling tests

You can debug the scanner when running ruling tests. As a new JVM is spawned to run the analysis you can't simply click 'debug' on a ruling
test, however. You need to tell the Sonar Scanner (which is being used to run the analysis in the background) to launch a debuggable JVM.
Then you can attach to this JVM instance and debug as normal via your IDE.

The ruling test already provides a convenient API where all you need to do is supply the port you want to debug on (e.g. 5005)
to `sonar.rulingDebugPort`. So, for instance, if you start the ruling tests from the CLI, run:

    ./gradlew :its:ruling:test -Pruling --info --console=plain --no-daemon -Dsonar.rulingDebugPort=5005

You can obviously do the same in the IDE and/or only run a particular test:

    :its:ruling:test -Pruling --info --console=plain --tests "org.sonarsource.slang.SlangRulingTest.test_kotlin_corda" -Dsonar.rulingDebugPort=5005

## Utilities and Developing

### Generating/downloading rule metadata

The Gradle task `generateRuleMetadata` will download the rule metadata from the [RSPEC repository](https://github.com/SonarSource/rspec/).

For example, execute the following in the project root to fetch the metadata for rule `S42`:

    ./gradlew generateRuleMetadata -PruleKey=S42

If fetching from a branch:

    ./gradlew generateRuleMetadata -PruleKey=S4830 -Pbranch=a_branch

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

    ./gradlew updateRuleMetadata

### Implementing a new rule

The Gradle task `setupRuleStubs` will create the commonly required files for implementing a new rule, including usual boilerplate code. It
will also put the rule into the list of checks and call `generateRuleMetadata` to download the rule's metadata.

To use this task, you need to know the rule key and a fitting name for the check class. For instance, if you want to implement the new
rule `S42` in the class `AnswersEverythingCheck`, you can call the following in the root of the project:

    ./gradlew setupRuleStubs -PruleKey=S42 -PclassName=AnswersEverythingCheck

To create stubs for Kotlin Gradle DSL rules instead of Kotlin rules, use `setupGradleRuleStubs`, as in: 

    ./gradlew setupGradleRuleStubs -PruleKey=S6626 -PclassName=TaskDefinitionsCheck

### Updating external linter rule mappings

See [this README in the utils](utils-kotlin/README.md).

### Visualizing ASTs

If you want a graphical output of ASTs, see [this README in the utils](utils-kotlin/README.md) for more info on how to convert an AST into a
DOT format.
