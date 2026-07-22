import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

// The scanner-engine test-fixture artifacts (testImplementation below) require JVM 17+;
// bump only the test compilation target, main stays at the repo-wide baseline.
tasks.withType<JavaCompile>().matching { it.name == "compileTestJava" }.configureEach {
    options.release.set(17)
}
tasks.withType<KotlinCompile>().matching { it.name == "compileTestKotlin" }.configureEach {
    compilerOptions.jvmTarget = JvmTarget.JVM_17
    compilerOptions.freeCompilerArgs.add("-Xjdk-release=17")
}

dependencies {
    compileOnly(libs.sonar.plugin.api)
    implementation(libs.sonar.analyzer.commons)
    implementation(libs.sonar.xml.parsing)
    implementation(libs.sonar.regex.parsing)
    implementation(libs.sonar.performance.measure)
    implementation(libs.kotlin.compiler)
    implementation(libs.staxmate)
    implementation(libs.gson)
    implementation(libs.sonar.analyzer.commons.recognizers)

    implementation(project(":sonar-kotlin-api"))

    testImplementation(testLibs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(testLibs.assertj.core)
    testImplementation(testLibs.mockito.core)
    testImplementation(testLibs.mockk)
    testImplementation(testLibs.sonar.analyzer.test.commons)
    testImplementation(testLibs.sonar.plugin.api.scanner.impl) {
        // Exclude the transitive guava it pulls in (33.6.0-jre) — it conflicts with the older guava
        // (30.1.1-jre) bundled by kotlin-checks-test-sources on the K2 analysis classpath, breaking
        // semantic resolution for Guava-related checks (e.g. ReplaceGuavaWithKotlinCheck).
        exclude(group = "com.google.guava", module = "guava")
    }
    testImplementation(testLibs.sonar.sensor.test.fixtures) {
        exclude(group = "com.google.guava", module = "guava")
    }
    testImplementation(testLibs.logback.classic)
    testImplementation(testLibs.sonar.plugin.api.test.fixtures)

    testImplementation(project(":sonar-kotlin-test-api"))
}

val test: Test by tasks
test.dependsOn(project(":kotlin-checks-test-sources").tasks.named("build"))
