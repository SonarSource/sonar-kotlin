dependencies {
    testImplementation(testLibs.sonarlint.core)
    testImplementation(testLibs.sonar.orchestrator)
    testImplementation(testLibs.assertj.core)
    testImplementation(testLibs.sonar.ws)
    testImplementation(libs.sonar.analyzer.commons)
}

sonarqube.isSkipProject = true

tasks.test {
    onlyIf {
        project.hasProperty("plugin") || project.hasProperty("its")
    }
    filter {
        includeTestsMatching("org.sonarsource.slang.Tests")
        includeTestsMatching("org.sonarsource.slang.SonarLintTest")
    }
    systemProperty("java.awt.headless", "true")
    outputs.upToDateWhen { false }
}
