plugins {
    // include kotlin in the source main classpath exported bellow as "gradle.main.compile.classpath"
    kotlin("jvm")
}

dependencies {
    testImplementation(testLibs.sonar.orchestrator)
    testImplementation(testLibs.assertj.core)
    testImplementation(libs.sonar.analyzer.commons)
}

sonarqube.isSkipProject = true

tasks.test {
    onlyIf {
        project.hasProperty("its") || project.hasProperty("ruling")
    }
    listOf("keepSonarqubeRunning", "reportAll", "cleanProjects", "buildProjects")
        .associateWith { System.getProperty(it) }
        .filter { it.value != null }
        .forEach { systemProperty(it.key, it.value) }
    systemProperty("java.awt.headless", "true")
    // export a classpath containing kotlin standard dependencies
    systemProperty("gradle.main.compile.classpath", sourceSets.main.get().compileClasspath.asPath)
    outputs.upToDateWhen { false }
}
