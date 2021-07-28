dependencies {
    testImplementation("org.sonarsource.orchestrator:sonar-orchestrator")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.sonarsource.analyzer-commons:sonar-analyzer-commons")
}

sonarqube.isSkipProject = true

tasks.test {
    onlyIf {
        project.hasProperty("its") ||
            project.hasProperty("ruling") ||
            project.hasProperty("ruling-kotlin") ||
            project.hasProperty("ruling-ruby") ||
            project.hasProperty("ruling-scala") ||
            project.hasProperty("ruling-go")
    }
    if (project.hasProperty("ruling-kotlin")) {
        filter { includeTestsMatching("org.sonarsource.slang.SlangRulingTest.test_kotlin*") }
    } else if (project.hasProperty("ruling-ruby")) {
        filter { includeTestsMatching("org.sonarsource.slang.SlangRulingTest.test_ruby") }
    } else if (project.hasProperty("ruling-scala")) {
        filter { includeTestsMatching("org.sonarsource.slang.SlangRulingTest.test_scala") }
    } else if (project.hasProperty("ruling-go")) {
        filter { includeTestsMatching("org.sonarsource.slang.SlangRulingTest.test_go") }
    }
    systemProperty("java.awt.headless", "true")
    outputs.upToDateWhen { false }
}
