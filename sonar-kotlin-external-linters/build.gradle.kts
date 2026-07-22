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
    implementation(libs.kotlin.compiler)
    implementation(libs.staxmate)
    implementation(libs.gson)
    implementation(libs.sonar.analyzer.commons.recognizers)

    implementation(project(":sonar-kotlin-api"))

    testImplementation(testLibs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.slf4j.api)
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
