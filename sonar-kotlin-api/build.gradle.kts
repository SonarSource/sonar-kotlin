plugins {
    kotlin("jvm")
}

dependencies {
    listOf(
        // Source of these artifacts is
        // https://github.com/JetBrains/kotlin/tree/v2.0.21/prepare/ide-plugin-dependencies
        // where ones whose name contains "high-level" are deprecated and should not be used - see
        // https://github.com/JetBrains/kotlin/commit/3ad9798a17ad9eb68cdb1e9f8f1a69584151bfd4
        "org.jetbrains.kotlin:analysis-api-standalone-for-ide",
        "org.jetbrains.kotlin:analysis-api-platform-interface-for-ide",
        "org.jetbrains.kotlin:analysis-api-for-ide", // old name "high-level-api-for-ide"
        "org.jetbrains.kotlin:analysis-api-impl-base-for-ide", // old name "high-level-api-impl-base"
        "org.jetbrains.kotlin:analysis-api-k2-for-ide", // old name "high-level-api-k2"
        "org.jetbrains.kotlin:low-level-api-fir-for-ide",
        "org.jetbrains.kotlin:symbol-light-classes-for-ide"
    ).forEach {
        val kotlinVersion: String by project.ext
        api("$it:$kotlinVersion") {
            // https://youtrack.jetbrains.com/issue/KT-61639/Standalone-Analysis-API-cannot-find-transitive-dependencies
            isTransitive = false
        }
    }
    implementation("com.github.ben-manes.caffeine:caffeine:2.9.3")

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

    testImplementation(testLibs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.slf4j.api)
    testImplementation(testLibs.assertj.core)
    testImplementation(testLibs.mockito.core)
    testImplementation(testLibs.mockk)
    testImplementation(testLibs.sonar.analyzer.test.commons)
    testImplementation(testLibs.sonar.plugin.api.impl)
    testImplementation(testLibs.sonar.plugin.api.test.fixtures)
    testImplementation(project(":sonar-kotlin-test-api"))
}

val test: Test by tasks
test.dependsOn(project(":kotlin-checks-test-sources").tasks.named("build"))

task<JavaExec>("printAst") {
    group = "Application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.sonarsource.kotlin.ast.AstPrinterKt")
}
