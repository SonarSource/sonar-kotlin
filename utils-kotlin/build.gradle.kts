plugins {
    java
    application
    kotlin("jvm")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(utilLibs.bundles.detekt)
    implementation(utilLibs.bundles.ktlint)
    runtimeOnly("org.slf4j:slf4j-simple:1.6.1")

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
