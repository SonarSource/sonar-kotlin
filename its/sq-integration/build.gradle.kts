plugins {
    id("org.sonarsource.cloud-native.integration-test")
}

dependencies {
    integrationTestImplementation(testLibs.sonarlint.core.test.utils) {
        // The backend harness does not use the WebSocket test helper.
        exclude(group = "org.java-websocket", module = "Java-WebSocket")
    }
    integrationTestImplementation(testLibs.awaitility)
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
}

sonarqube.isSkipProject = true
