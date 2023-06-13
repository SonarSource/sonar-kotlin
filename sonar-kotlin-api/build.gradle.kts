import java.io.File
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.jar.JarInputStream

plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(libs.sonar.plugin.api)
    implementation(libs.sonar.analyzer.commons)
    implementation(libs.sonar.xml.parsing)
    implementation(libs.sonar.regex.parsing)
    implementation(libs.sonar.performance.measure)
    implementation(libs.kotlin.compiler.embeddable)
    implementation(libs.staxmate)
    implementation(libs.gson)
    implementation(libs.sonar.analyzer.commons.recognizers)

    testImplementation(testLibs.junit.api)
    testImplementation(testLibs.junit.params)
    testRuntimeOnly(testLibs.junit.engine)
    testImplementation(testLibs.assertj.core)
    testImplementation(testLibs.mockito.core)
    testImplementation(testLibs.mockk)
    testImplementation(testLibs.classgraph)
    testImplementation(testLibs.sonar.analyzer.test.commons)
    testImplementation(testLibs.sonar.plugin.api.impl)
    testImplementation(testLibs.sonar.plugin.api.test.fixtures)
}

/* TODO: remove?
tasks.withType<JavaCompile> {
    // Prevent warning: Gradle 5.0 will ignore annotation processors
    options.compilerArgs = options.compilerArgs + "-proc:none"
}*/

// TODO: check if it has been fixed + backported to JDK 11 (note: has been fixed for JDK 13)
// The new version 11.0.17 of javadoc has a bug and does not handle package annotations correctly
// Adding a "tag" option is a workaround to prevent javadoc errors
// @see https://bugs.openjdk.org/browse/JDK-8295850
tasks.withType<Javadoc> {
    options {
        this as StandardJavadocDocletOptions
        addStringOption("tag", "javax.annotation.ParametersAreNonnullByDefault:ParametersAreNonnullByDefault")
    }
}
