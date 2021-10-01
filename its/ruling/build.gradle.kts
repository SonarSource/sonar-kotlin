plugins {
  // include kotlin in the source main classpath exported bellow as "gradle.main.compile.classpath"
  kotlin("jvm")
}

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
    // export a classpath containing kotlin standard dependencies
    systemProperty( "gradle.main.compile.classpath", sourceSets.main.get().compileClasspath.asPath)
    outputs.upToDateWhen { false }
}
