import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

// The scanner-engine test-fixture artifacts this module depends on (compileOnly) require JVM 17+.
// This module is only ever consumed via testImplementation (never packaged into the shipped plugin jar),
// so it's safe to compile it at a higher bytecode target than the rest of the plugin.
java.sourceCompatibility = JavaVersion.VERSION_17

tasks.withType<JavaCompile> {
    options.release.set(17)
}

tasks.withType<KotlinCompile>().all {
    compilerOptions.jvmTarget = JvmTarget.JVM_17
    compilerOptions.freeCompilerArgs.add("-Xjdk-release=17")
}

dependencies {
    compileOnly(libs.sonar.plugin.api)
    compileOnly(testLibs.junit.jupiter)
    compileOnly(testLibs.assertj.core)
    compileOnly(testLibs.mockito.core)
    compileOnly(testLibs.mockk)
    compileOnly(testLibs.sonar.analyzer.test.commons)
    compileOnly(testLibs.sonar.plugin.api.scanner.impl) {
        // Exclude the transitive guava it pulls in (33.6.0-jre) — it conflicts with the older guava
        // (30.1.1-jre) bundled by kotlin-checks-test-sources on the K2 analysis classpath, breaking
        // semantic resolution for Guava-related checks (e.g. ReplaceGuavaWithKotlinCheck).
        exclude(group = "com.google.guava", module = "guava")
    }
    compileOnly(testLibs.sonar.sensor.test.fixtures) {
        exclude(group = "com.google.guava", module = "guava")
    }
    testImplementation(testLibs.logback.classic)
    compileOnly(testLibs.sonar.plugin.api.test.fixtures)
    compileOnly(project(":sonar-kotlin-api"))

    implementation(libs.sonar.analyzer.commons)
    implementation(libs.sonar.xml.parsing)
    implementation(libs.sonar.regex.parsing)
    implementation(libs.sonar.performance.measure)
    implementation(libs.kotlin.compiler)
    implementation(libs.staxmate)
    implementation(libs.gson)
    implementation(libs.sonar.analyzer.commons.recognizers)
}
