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

    implementation(libs.analysis.api.providers.`for`.ide)
    implementation(libs.analysis.api.standalone.`for`.ide)
    implementation(libs.high.level.api.`for`.ide)
    implementation(libs.high.level.api.fir.`for`.ide)
    implementation(libs.symbol.light.classes.`for`.ide)
    implementation(libs.high.level.api.impl.base.`for`.ide)
    implementation(libs.kotlin.compiler.common.`for`.ide)
    implementation(libs.kotlin.compiler.fir.`for`.ide)
    implementation(libs.kotlin.compiler.fe10.`for`.ide)
    implementation(libs.kotlin.compiler.ir.`for`.ide)

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
