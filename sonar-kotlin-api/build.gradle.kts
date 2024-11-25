plugins {
    kotlin("jvm")
}

dependencies {
    // TODO https://github.com/JetBrains/kotlin/commit/3ad9798a17ad9eb68cdb1e9f8f1a69584151bfd4
    listOf(
        // TODO high-level-api => analysis-api ?
        "org.jetbrains.kotlin:high-level-api-for-ide",
        "org.jetbrains.kotlin:analysis-api-fe10-for-ide",
//        "org.jetbrains.kotlin:analysis-api-k2-for-ide", // not needed ?
        // TODO high-level-api => analysis-api-k2 ?
        "org.jetbrains.kotlin:high-level-api-fir-for-ide", // needed for K2
//        "org.jetbrains.kotlin:high-level-api-for-ide", // not needed ?
        "org.jetbrains.kotlin:low-level-api-fir-for-ide", // needed for K2
//        "org.jetbrains.kotlin:analysis-project-structure-for-ide", // not needed ?
        "org.jetbrains.kotlin:symbol-light-classes-for-ide",
        "org.jetbrains.kotlin:analysis-api-standalone-for-ide",
        "org.jetbrains.kotlin:analysis-api-platform-interface-for-ide",
        // TODO high-level-api-impl-base => analysis-api-impl-base ?
        "org.jetbrains.kotlin:high-level-api-impl-base-for-ide"
//        "org.jetbrains.kotlin:analysis-api-for-ide" // not needed ?
    ).forEach {
        val kotlinVersion: String by project.ext
        // https://youtrack.jetbrains.com/issue/KT-61639/Standalone-Analysis-API-cannot-find-transitive-dependencies
        api("$it:$kotlinVersion") { isTransitive = false }
    }

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
    implementation("com.github.ben-manes.caffeine:caffeine:2.9.3")

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

task<JavaExec>("printAst") {
    group = "Application"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("org.sonarsource.kotlin.ast.AstPrinterKt")
}
