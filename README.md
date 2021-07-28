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
* [SonarQube Community Forum](https://community.sonarsource.com/)


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

