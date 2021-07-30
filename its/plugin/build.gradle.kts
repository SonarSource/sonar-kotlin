dependencies {
    testImplementation("org.sonarsource.sonarlint.core:sonarlint-core")
    testImplementation("org.sonarsource.orchestrator:sonar-orchestrator")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.sonarsource.sonarqube:sonar-ws")
    testImplementation("org.sonarsource.analyzer-commons:sonar-analyzer-commons")
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
