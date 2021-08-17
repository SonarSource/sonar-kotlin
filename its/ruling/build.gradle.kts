dependencies {
    testImplementation("org.sonarsource.orchestrator:sonar-orchestrator")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.sonarsource.analyzer-commons:sonar-analyzer-commons")
}

sonarqube.isSkipProject = true

tasks.test {
    onlyIf {
        project.hasProperty("its") || project.hasProperty("ruling")
    }
    systemProperty("java.awt.headless", "true")
    outputs.upToDateWhen { false }
}
