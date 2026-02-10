plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly(libs.sonar.plugin.api)
    compileOnly(libs.slf4j.api)
    // force the usage of stax2-api, which otherwise gets substituted by a stripped down version embedded into kotlin-compiler
    api(libs.stax2.api)
    implementation(project(":sonar-kotlin-metrics"))
    implementation(libs.sonar.analyzer.commons)
    implementation(libs.sonar.xml.parsing)
    implementation(libs.sonar.regex.parsing)
    implementation(libs.sonar.performance.measure)
    implementation(libs.staxmate)
    implementation(libs.gson)
    implementation(libs.sonar.analyzer.commons.recognizers)

    testImplementation(testLibs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.slf4j.api)
    testImplementation(testLibs.assertj.core)
    testImplementation(testLibs.mockito.core)
    testImplementation(testLibs.mockk)
    testImplementation(testLibs.sonar.analyzer.test.commons)
    testImplementation(testLibs.sonar.plugin.api.impl)
    testImplementation(testLibs.sonar.plugin.api.test.fixtures)
}
