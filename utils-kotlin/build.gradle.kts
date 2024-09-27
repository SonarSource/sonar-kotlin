plugins {
    java
    application
    kotlin("jvm")
}

repositories {
    maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(utilLibs.bundles.detekt)
    implementation(utilLibs.bundles.ktlint)

    implementation(project(":sonar-kotlin-external-linters")) {
        // detekt needs strict specific version of kotlin-compiler-embeddable which differs from our
        isTransitive = false
    }

    implementation(libs.gson)
    implementation(utilLibs.jcommander)

    testImplementation(testLibs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(testLibs.assertj.core)
    testImplementation(libs.sonar.plugin.api)
}

tasks {
    task<JavaExec>("updateDetektRules") {
        group = "Application"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("org.sonarsource.kotlin.externalreport.detekt.DetektRuleDefinitionGeneratorKt")

        doFirst {
            println("Updating rules for Detekt version ${utilLibs.versions.detekt.get()}...")
        }
    }

    task<JavaExec>("updateKtlintRules") {
        group = "Application"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("org.sonarsource.kotlin.externalreport.ktlint.KtlintRuleDefinitionGeneratorKt")

        doFirst {
            println("Updating rules for ktlint version ${utilLibs.versions.ktlint.get()}...")
        }
    }

    task<JavaExec>("updateAndroidLintRules") {
        group = "Application"
        classpath = sourceSets.main.get().runtimeClasspath
        mainClass.set("org.sonarsource.kotlin.externalreport.androidlint.AndroidLintRuleDefinitionKt")

        doFirst {
            println("Updating rules for Android Lint")
        }
    }
}
