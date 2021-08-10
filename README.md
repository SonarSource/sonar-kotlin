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

By default, Integration Tests (ITs) are skipped during the build.
If you want to run them, you need first to retrieve the related projects which are used as input:

    git submodule update --init its/sources
    cd its/ruling/kotlin/ktor
    
Then you need to switch to Java8 and run the command to generate binaries for Ktor project:

    ./gradlew assemble

Then build and run the Integration Tests using the `its` property:

    ./gradlew build -Pits --info --no-daemon -Dsonar.runtimeVersion=7.9

You can also build and run only Ruling Tests using the `ruling` property:

    ./gradlew build -Pruling --info --no-daemon -Dsonar.runtimeVersion=7.9

You can also build and run only Plugin Tests using the `plugin` property:

    ./gradlew build -Pplugin --info --no-daemon -Dsonar.runtimeVersion=7.9

## Utilities and Developing
### Generating/downloading rule metadata
The Gradle task `generateRuleMetadata` will download the rule metadata from the [RSPEC repository](https://github.com/SonarSource/rspec/).

For example, execute the following in the project root to fetch the metadata for rule `S42`:

    ./gradlew generateRuleMetadata -PruleKey=S42

If you want to update all rules' metadata, you can use:
    
    ./gradlew updateRuleMetadata

### Implementing a new rule
The Gradle task `setupRuleStubs` will create the commonly required files for implementing a new rule, including usual boilerplate code. It will also put the rule into the list of checks and call `generateRuleMetadata` to download the rule's metadata.

To use this task, you need to know the rule key and a fitting name for the check class. For instance, if you want to implement the new rule `S42` in the class `AnswersEverythingCheck`, you can call the following in the root of the project:

    ./gradlew setupRuleStubs -PruleKey=S42 -PclassName=AnswersEverythingCheck
