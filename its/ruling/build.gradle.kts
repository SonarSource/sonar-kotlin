plugins {
    // include kotlin in the source main classpath exported bellow as "gradle.main.compile.classpath"
    kotlin("jvm")
}

val sonarKotlinPluginDist by configurations.creating {
    isCanBeConsumed = false
    isCanBeResolved = true
    isTransitive = false
    attributes {
        attribute(Attribute.of("org.sonarsource.kotlin.dist", String::class.java), "sonar-kotlin-plugin")
    }
}

dependencies {
    sonarKotlinPluginDist(project(":sonar-kotlin-plugin"))
    testImplementation(testLibs.sonar.orchestrator.junit5)
    testImplementation(testLibs.assertj.core)
    testImplementation(testLibs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.sonar.analyzer.commons)
}

sonarqube.isSkipProject = true

tasks.test {
    useJUnitPlatform()
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

    inputs.files(sonarKotlinPluginDist)
}
