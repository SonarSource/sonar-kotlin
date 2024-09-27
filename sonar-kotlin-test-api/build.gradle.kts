plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(libs.sonar.plugin.api)
    compileOnly(testLibs.junit.jupiter)
    compileOnly(testLibs.assertj.core)
    compileOnly(testLibs.mockito.core)
    compileOnly(testLibs.mockk)
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
