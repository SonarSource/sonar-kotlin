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
    // Each SIT run leaks engine class loaders; a fresh JVM per class keeps heap bounded.
    setForkEvery(1)
    // One test class per corpus (see AbstractKotlinRulingTest), each forked into its own JVM/classloader, so
    // corpora now run in parallel across forks. JUnit-level parallelism within a single fork is still not an
    // option; see junit-platform.properties for why (shared, non-thread-safe engine state).
    maxParallelForks = 2
    // The engine now runs inside this JVM (the orchestrator used to fork a scanner process with its own -Xmx),
    // and the ruling corpus needs considerably more than the Gradle default. Sized down from 4g so that two
    // concurrent forks don't exceed what a single fork used to request on its own; the heaviest corpus
    // (test_kotlin_compiler, CI-only) crashed with internal K2/FIR analysis errors under 2x4g concurrent
    // forks on the CI runner (see qa_ruling failure on a64d4e4b). Kept closer to 4g than a straight half,
    // since that corpus previously ran comfortably at 4g with zero contention.
    maxHeapSize = "3g"
}

sonarqube.isSkipProject = true
