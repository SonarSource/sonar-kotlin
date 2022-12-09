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

include("sonar-kotlin-plugin")
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
        val analyzerCommonsVersionStr = "2.1.0.1111"

        create("libs") {
            val analyzerCommons = version("analyzerCommons", analyzerCommonsVersionStr)
            val gson = version("gson", "2.9.0")
            val sonarPluginApi = version("sonarPluginApi", "9.12.0.310")
            val staxmate = version("staxmate", "2.4.0")

            library("gson", "com.google.code.gson", "gson").versionRef(gson)
            library("kotlin-compiler-embeddable", "org.jetbrains.kotlin", "kotlin-compiler-embeddable").version(kotlinVersion)
            library("sonar-analyzer-commons", "org.sonarsource.analyzer-commons", "sonar-analyzer-commons").versionRef(analyzerCommons)
            library("sonar-analyzer-commons-recognizers", "org.sonarsource.analyzer-commons", "sonar-analyzer-recognizers").versionRef(analyzerCommons)
            library("sonar-performance-measure", "org.sonarsource.analyzer-commons", "sonar-performance-measure").versionRef(analyzerCommons)
            library("sonar-plugin-api", "org.sonarsource.api.plugin", "sonar-plugin-api").versionRef(sonarPluginApi)
            library("sonar-regex-parsing", "org.sonarsource.analyzer-commons", "sonar-regex-parsing").versionRef(analyzerCommons)
            library("sonar-xml-parsing", "org.sonarsource.analyzer-commons", "sonar-xml-parsing").versionRef(analyzerCommons)
            library("staxmate", "com.fasterxml.staxmate", "staxmate").versionRef(staxmate)
        }

        create("utilLibs") {
            val detekt = version("detekt", "1.22.0")
            val jcommander = version("jcommander", "1.81")
            val ktlint = version("ktlint", "0.47.1")

            library("detekt-api", "io.gitlab.arturbosch.detekt", "detekt-api").versionRef(detekt)
            library("detekt-cli", "io.gitlab.arturbosch.detekt", "detekt-cli").versionRef(detekt)
            library("detekt-core", "io.gitlab.arturbosch.detekt", "detekt-core").versionRef(detekt)
            library("jcommander", "com.beust", "jcommander").versionRef(jcommander)
            library("ktlint", "com.pinterest", "ktlint").versionRef(ktlint)
            library("ktlint-core", "com.pinterest.ktlint", "ktlint-core").versionRef(ktlint)
            library("ktlint-ruleset-experimental", "com.pinterest.ktlint", "ktlint-ruleset-experimental").versionRef(ktlint)
            library("ktlint-ruleset-standard", "com.pinterest.ktlint", "ktlint-ruleset-standard").versionRef(ktlint)

            bundle("detekt", listOf("detekt-cli", "detekt-core", "detekt-api"))
            bundle("ktlint", listOf("ktlint", "ktlint-core", "ktlint-ruleset-standard", "ktlint-ruleset-experimental"))
        }

        create("testLibs") {
            val analyzerCommons = version("analyzerCommons", analyzerCommonsVersionStr)
            val assertj = version("assertj", "3.23.1")
            val classgraph = version("classgraph", "4.8.149")
            val junit = version("junit", "5.8.2")
            val mockito = version("mockito", "4.6.1")
            val mockk = version("mockk", "1.12.4")
            val orchestrator = version("orchestrator", "3.40.0.183")
            val sonarlint = version("sonarlint", "7.0.0.37656")
            val sonarqube = version("sonarqube", "9.7.1.62043")

            library("assertj-core", "org.assertj", "assertj-core").versionRef(assertj)
            library("classgraph", "io.github.classgraph", "classgraph").versionRef(classgraph)
            library("junit-api", "org.junit.jupiter", "junit-jupiter-api").versionRef(junit)
            library("junit-engine", "org.junit.jupiter", "junit-jupiter-engine").versionRef(junit)
            library("junit-params", "org.junit.jupiter", "junit-jupiter-params").versionRef(junit)
            library("mockito-core", "org.mockito", "mockito-core").versionRef(mockito)
            library("mockk", "io.mockk", "mockk").versionRef(mockk)
            library("sonar-analyzer-test-commons", "org.sonarsource.analyzer-commons", "sonar-analyzer-test-commons").versionRef(analyzerCommons)
            library("sonar-orchestrator", "org.sonarsource.orchestrator", "sonar-orchestrator").versionRef(orchestrator)
            library("sonar-plugin-api-impl", "org.sonarsource.sonarqube", "sonar-plugin-api-impl").versionRef(sonarqube)
            library("sonar-ws", "org.sonarsource.sonarqube", "sonar-ws").versionRef(sonarqube)
            library("sonarlint-core", "org.sonarsource.sonarlint.core", "sonarlint-core").versionRef(sonarlint)
        }
    }
}
