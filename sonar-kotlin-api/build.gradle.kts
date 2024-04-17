import org.gradle.kotlin.dsl.provider.inLenientMode
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(libs.sonar.plugin.api)
    compileOnly(libs.slf4j.api)
    implementation(libs.sonar.analyzer.commons)
    implementation(libs.sonar.xml.parsing)
    implementation(libs.sonar.regex.parsing)
    implementation(libs.sonar.performance.measure)
    implementation(libs.kotlin.compiler.embeddable)
    implementation(libs.staxmate)
    implementation(libs.gson)
    implementation(libs.sonar.analyzer.commons.recognizers)

    implementation("com.jetbrains.intellij.platform:util:232.4652")
    implementation("com.jetbrains.intellij.platform:core:232.4652")
    implementation("com.jetbrains.intellij.platform:core-impl:232.4652")
    implementation("com.jetbrains.intellij.java:java-psi:232.4652")

    implementation("org.jetbrains.kotlin:high-level-api-for-ide:2.0.0-RC1")
    implementation("org.jetbrains.kotlin:kotlin-compiler-common-for-ide:2.0.0-RC1")
    implementation("org.jetbrains.kotlin:kotlin-compiler-fe10-for-ide:2.0.0-RC1")

    testRuntimeOnly(testLibs.junit.engine)
    testImplementation(libs.slf4j.api)
    testImplementation(testLibs.junit.api)
    testImplementation(testLibs.junit.params)
    testImplementation(testLibs.assertj.core)
    testImplementation(testLibs.mockito.core)
    testImplementation(testLibs.mockk)
    testImplementation(testLibs.classgraph)
    testImplementation(testLibs.sonar.analyzer.test.commons)
    testImplementation(testLibs.sonar.plugin.api.impl)
    testImplementation(testLibs.sonar.plugin.api.test.fixtures)
    testImplementation(project(":sonar-kotlin-test-api"))
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        freeCompilerArgs.add("-Xopt-in=org.jetbrains.kotlin.analysis.api.analyze.kt")
    }
}

// The new version 11.0.17 of javadoc has a bug and does not handle package annotations correctly
// Adding a "tag" option is a workaround to prevent javadoc errors
// @see https://bugs.openjdk.org/browse/JDK-8295850
tasks.withType<Javadoc> {
    options {
        this as StandardJavadocDocletOptions
        addStringOption("tag", "javax.annotation.ParametersAreNonnullByDefault:ParametersAreNonnullByDefault")
    }
}
