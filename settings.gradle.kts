pluginManagement {
    repositories {
        maven(url = "https://repox.jfrog.io/repox/plugins.gradle.org/")
        gradlePluginPortal()
    }

    val kotlinVersion: String by settings
    plugins {
        id("org.jetbrains.kotlin.jvm") version kotlinVersion
    }
}

rootProject.name = "kotlin"

include("sonar-kotlin-api")
include("sonar-kotlin-test-api")
include("sonar-kotlin-checks")
include("sonar-kotlin-external-linters")
include("sonar-kotlin-surefire")
include("sonar-kotlin-metrics")
include("sonar-kotlin-plugin")
include("sonar-kotlin-gradle")

include("its:plugin")
include("its:ruling")
include("kotlin-checks-test-sources")
include("utils-kotlin")

dependencyResolutionManagement {

    /*
    * We are knowingly using this versionCatalogs feature, as it improves dependency management drastically, even though it is still marked
    * as unstable.
    */
    @Suppress("UnstableApiUsage")
    versionCatalogs {

        val kotlinVersion: String by extra
        val analyzerCommonsVersionStr = "2.7.0.1482"
        val sonarPluginApi = "10.3.0.1951"
        val slf4jApi = "1.7.30"

        create("libs") {
            val analyzerCommons = version("analyzerCommons", analyzerCommonsVersionStr)
            val gson = version("gson", "2.10.1")
            val staxmate = version("staxmate", "2.4.1")
            val gradleToolingApi = version("gradle-tooling-api", "7.5.1")

            library("gson", "com.google.code.gson", "gson").versionRef(gson)
            library("kotlin-compiler-embeddable", "org.jetbrains.kotlin", "kotlin-compiler-embeddable").version(kotlinVersion)
            library("sonar-analyzer-commons", "org.sonarsource.analyzer-commons", "sonar-analyzer-commons").versionRef(analyzerCommons)
            library("sonar-analyzer-commons-recognizers", "org.sonarsource.analyzer-commons", "sonar-analyzer-recognizers")
                .versionRef(analyzerCommons)
            library("sonar-performance-measure", "org.sonarsource.analyzer-commons", "sonar-performance-measure")
                .versionRef(analyzerCommons)
            library("sonar-plugin-api", "org.sonarsource.api.plugin", "sonar-plugin-api").version(sonarPluginApi)
            library("slf4j-api", "org.slf4j", "slf4j-api").version(slf4jApi)
            library("sonar-regex-parsing", "org.sonarsource.analyzer-commons", "sonar-regex-parsing").versionRef(analyzerCommons)
            library("sonar-xml-parsing", "org.sonarsource.analyzer-commons", "sonar-xml-parsing").versionRef(analyzerCommons)
            library("staxmate", "com.fasterxml.staxmate", "staxmate").versionRef(staxmate)
            library("gradle-tooling-api", "org.gradle", "gradle-tooling-api").versionRef(gradleToolingApi)
        }

        create("utilLibs") {
            val detekt = version("detekt", "1.23.3")
            val jcommander = version("jcommander", "1.82")
            val ktlint = version("ktlint", "1.0.1")

            library("detekt-api", "io.gitlab.arturbosch.detekt", "detekt-api").versionRef(detekt)
            library("detekt-cli", "io.gitlab.arturbosch.detekt", "detekt-cli").versionRef(detekt)
            library("detekt-core", "io.gitlab.arturbosch.detekt", "detekt-core").versionRef(detekt)
            library("jcommander", "com.beust", "jcommander").versionRef(jcommander)
            library("ktlint-core", "com.pinterest.ktlint", "ktlint-rule-engine-core").versionRef(ktlint)
            library("ktlint-ruleset-standard", "com.pinterest.ktlint", "ktlint-ruleset-standard").versionRef(ktlint)

            bundle("detekt", listOf("detekt-cli", "detekt-core", "detekt-api"))
            bundle("ktlint", listOf("ktlint-core", "ktlint-ruleset-standard"))
        }

        create("testLibs") {
            val analyzerCommons = version("analyzerCommons", analyzerCommonsVersionStr)
            val assertj = version("assertj", "3.24.2")
            val classgraph = version("classgraph", "4.8.165")
            val junit = version("junit", "5.10.1")
            val mockito = version("mockito", "5.7.0")
            val mockk = version("mockk", "1.13.3")
            val orchestrator = version("orchestrator", "4.5.0.1682")
            val sonarlint = version("sonarlint", "9.5.0.76302")
            val sonarqube = version("sonarqube", "10.0.0.68432")

            library("assertj-core", "org.assertj", "assertj-core").versionRef(assertj)
            library("classgraph", "io.github.classgraph", "classgraph").versionRef(classgraph)
            library("junit-api", "org.junit.jupiter", "junit-jupiter-api").versionRef(junit)
            library("junit-engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef(junit)
            library("junit-params", "org.junit.jupiter", "junit-jupiter-params").versionRef(junit)
            library("mockito-core", "org.mockito", "mockito-core").versionRef(mockito)
            library("mockk", "io.mockk", "mockk").versionRef(mockk)
            library("sonar-analyzer-test-commons", "org.sonarsource.analyzer-commons", "sonar-analyzer-test-commons")
                .versionRef(analyzerCommons)
            library("sonar-orchestrator-junit4", "org.sonarsource.orchestrator", "sonar-orchestrator-junit4").versionRef(orchestrator)
            library("sonar-plugin-api-impl", "org.sonarsource.sonarqube", "sonar-plugin-api-impl").versionRef(sonarqube)
            library("sonar-plugin-api-test-fixtures", "org.sonarsource.api.plugin", "sonar-plugin-api-test-fixtures")
                .version(sonarPluginApi)
            library("sonar-ws", "org.sonarsource.sonarqube", "sonar-ws").versionRef(sonarqube)
            library("sonarlint-core", "org.sonarsource.sonarlint.core", "sonarlint-core").versionRef(sonarlint)
        }
    }
}
