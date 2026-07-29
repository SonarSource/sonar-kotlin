plugins {
    id("org.sonarsource.cloud-native.integration-test")
}

dependencies {
    integrationTestImplementation(testLibs.sit)
    // Required: without it the engine fails with
    //   IllegalStateException: Unable to load components interface org.sonar.api.batch.sensor.Sensor
    integrationTestImplementation(testLibs.sonar.plugin.api.sit)
    // Required: without it ScannerMain.<clinit> throws
    //   FactoryConfigurationError: Provider for javax.xml.parsers.SAXParserFactory cannot be created
    integrationTestRuntimeOnly(testLibs.xerces.impl)
    integrationTestImplementation(testLibs.assertj.core)
    integrationTestImplementation(testLibs.junit.jupiter)
    integrationTestRuntimeOnly("org.junit.platform:junit-platform-launcher")
    integrationTestCompileOnly(libs.jsr305)
}

integrationTest {
    testSources.set(file("src/integrationTest/java"))
}

tasks.integrationTest {
    dependsOn(":sonar-kotlin-plugin:dist")
    // Each SIT run leaks engine class loaders; a fresh JVM per class keeps heap bounded.
    setForkEvery(1)
    // Peak = maxParallelForks * maxHeapSize = 4 * 1g = 4g.
    maxParallelForks = 4
    maxHeapSize = "1g"
}

sonarqube.isSkipProject = true
