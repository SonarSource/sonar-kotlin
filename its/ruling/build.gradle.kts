plugins {
    // include kotlin in the source main classpath exported below as "gradle.main.compile.classpath"
    kotlin("jvm")
    id("org.sonarsource.cloud-native.integration-test")
}

dependencies {
    integrationTestImplementation(testLibs.sit)
    // Required: without it the engine fails with
    //   IllegalStateException: Unable to load components interface org.sonar.api.batch.sensor.Sensor
    integrationTestImplementation(libs.sonar.plugin.api)
    // Required: without it ScannerMain.<clinit> throws
    //   FactoryConfigurationError: Provider for javax.xml.parsers.SAXParserFactory cannot be created
    integrationTestRuntimeOnly(testLibs.xerces.impl)
    integrationTestImplementation(testLibs.assertj.core)
    integrationTestImplementation(libs.gson)
    integrationTestImplementation(testLibs.junit.jupiter)
    integrationTestRuntimeOnly("org.junit.platform:junit-platform-launcher")
    integrationTestCompileOnly(libs.jsr305)
}

integrationTest {
    testSources.set(file("src/integrationTest/java"))
}

tasks.integrationTest {
    dependsOn(":sonar-kotlin-plugin:dist")
    listOf("reportAll")
        .associateWith { System.getProperty(it) }
        .filter { it.value != null }
        .forEach { systemProperty(it.key, it.value) }
    // export a classpath containing kotlin standard dependencies
    systemProperty("gradle.main.compile.classpath", sourceSets.main.get().compileClasspath.asPath)
    // Each SIT run leaks engine class loaders; a single shared JVM keeps heap bounded across the whole suite.
    setForkEvery(1)
    // The heaviest corpus (test_kotlin_compiler, CI-only) crashes the K2/FIR Analysis API with an internal
    // resolution error when it runs alone in a freshly-forked JVM. Splitting the corpora into separate test
    // classes (one fork each) reproduced this every time regardless of heap size or fork count; keeping all
    // corpora as @Test methods on one class run in a single fork avoids the cold-JVM path entirely.
    maxParallelForks = 1
    // The engine now runs inside this JVM (the orchestrator used to fork a scanner process with its own -Xmx),
    // and the ruling corpus needs considerably more than the Gradle default.
    maxHeapSize = "4g"
}

sonarqube.isSkipProject = true
