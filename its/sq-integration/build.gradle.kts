plugins {
    // include kotlin in the source main classpath exported below as "gradle.main.compile.classpath",
    // needed by the kotlin-language-server ruling corpus
    kotlin("jvm")
    id("org.sonarsource.cloud-native.integration-test")
}

dependencies {
    integrationTestImplementation(testLibs.sonarlint.core)
    integrationTestImplementation(testLibs.sonar.orchestrator.junit5)
    integrationTestImplementation(testLibs.junit.jupiter)
    integrationTestRuntimeOnly("org.junit.platform:junit-platform-launcher")
    integrationTestImplementation(testLibs.assertj.core)
    integrationTestImplementation(testLibs.sonar.ws)
    integrationTestImplementation(libs.sonar.analyzer.commons)
}

integrationTest {
    testSources.set(file("src/integrationTest/java"))
}

tasks.integrationTest {
    dependsOn(":sonar-kotlin-plugin:dist")
    listOf("keepSonarqubeRunning", "reportAll", "cleanProjects", "buildProjects")
        .associateWith { System.getProperty(it) }
        .filter { it.value != null }
        .forEach { systemProperty(it.key, it.value) }
    systemProperty("java.awt.headless", "true")
    // export a classpath containing kotlin standard dependencies, needed by the
    // kotlin-language-server ruling corpus for sonar.java.libraries
    systemProperty("gradle.main.compile.classpath", sourceSets.main.get().compileClasspath.asPath)
}

sonarqube.isSkipProject = true
