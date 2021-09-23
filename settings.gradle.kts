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
