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
    testImplementation(testLibs.sonarlint.core)
    testImplementation(testLibs.sonar.orchestrator.junit5)
    testImplementation(testLibs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(testLibs.assertj.core)
    testImplementation(testLibs.sonar.ws)
    testImplementation(libs.sonar.analyzer.commons)
}

sonarqube.isSkipProject = true

tasks.test {
    useJUnitPlatform()
    onlyIf {
        project.hasProperty("plugin") || project.hasProperty("its")
    }
    inputs.files(sonarKotlinPluginDist)
    systemProperty("java.awt.headless", "true")
    outputs.upToDateWhen { false }
}
