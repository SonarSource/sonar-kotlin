plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(libs.sonar.plugin.api)
    compileOnly(testLibs.junit.api)
    compileOnly(testLibs.junit.params)
    compileOnly(testLibs.assertj.core)
    compileOnly(testLibs.mockito.core)
    compileOnly(testLibs.mockk)
    compileOnly(testLibs.classgraph)
    compileOnly(testLibs.sonar.analyzer.test.commons)
    compileOnly(testLibs.sonar.plugin.api.impl)
    compileOnly(testLibs.sonar.plugin.api.test.fixtures)
    compileOnly(project(":sonar-kotlin-api"))

    implementation(libs.sonar.analyzer.commons)
    implementation(libs.sonar.xml.parsing)
    implementation(libs.sonar.regex.parsing)
    implementation(libs.sonar.performance.measure)
    implementation(libs.kotlin.compiler.embeddable)
    implementation(libs.staxmate)
    implementation(libs.gson)
    implementation(libs.sonar.analyzer.commons.recognizers)
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
