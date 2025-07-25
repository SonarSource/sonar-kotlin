import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.jar.JarInputStream

plugins {
    id("com.gradleup.shadow") version "8.3.1"
    kotlin("jvm")
    id("jacoco-report-aggregation")
}

buildscript {
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.6.1")
    }
}

dependencies {
    compileOnly(libs.sonar.plugin.api)
    compileOnly(libs.slf4j.api)
    implementation(libs.sonar.analyzer.commons)
    implementation(libs.sonar.xml.parsing)
    implementation(libs.sonar.regex.parsing)
    implementation(libs.sonar.performance.measure)
    implementation(libs.kotlin.compiler)
    implementation(libs.staxmate)
    implementation(libs.gson)
    implementation(libs.sonar.analyzer.commons.recognizers)

    implementation(project(":sonar-kotlin-api"))
    implementation(project(":sonar-kotlin-metrics"))
    implementation(project(":sonar-kotlin-external-linters"))
    implementation(project(":sonar-kotlin-gradle"))
    implementation(project(":sonar-kotlin-surefire"))
    implementation(project(":sonar-kotlin-checks"))

    testImplementation(testLibs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(libs.slf4j.api)
    testImplementation(testLibs.assertj.core)
    testImplementation(testLibs.mockito.core)
    testImplementation(testLibs.mockk)
    testImplementation(testLibs.sonar.analyzer.test.commons)
    testImplementation(testLibs.sonar.plugin.api.impl)
    testImplementation(testLibs.sonar.plugin.api.test.fixtures)

    testImplementation(project(":sonar-kotlin-test-api"))
    jacocoAggregation(project(":sonar-kotlin-test-api"))
}

val test: Test by tasks
test.dependsOn(project(":kotlin-checks-test-sources").tasks.named("build"))

tasks.jar {
    manifest {
        val displayVersion = if (project.property("buildNumber") == null) {
            project.version
        } else {
            project.version.toString()
                .substring(0, project.version.toString().lastIndexOf(".")) + " (build ${project.property("buildNumber")})"
        }
        val buildDate = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ").withZone(ZoneId.systemDefault()).format(Date().toInstant())
        attributes(
            mapOf(
                "Build-Time" to buildDate,
                "Implementation-Build" to ProcessBuilder().also { it.command("git", "rev-parse", "HEAD") }
                    .start().inputStream.bufferedReader().use {
                        it.readText().trim()
                    },
                "Plugin-BuildDate" to buildDate,
                // Note that use of ChildFirstClassLoader is deprecated since SonarQube 7.9
                "Plugin-ChildFirstClassLoader" to "false",
                "Plugin-Class" to "org.sonarsource.kotlin.plugin.KotlinPlugin",
                "Plugin-Description" to "Code Analyzer for Kotlin",
                "Plugin-Developers" to "SonarSource Team",
                "Plugin-Display-Version" to displayVersion,
                "Plugin-Homepage" to "https://redirect.sonarsource.com/plugins/kotlin.html",
                "Plugin-IssueTrackerUrl" to "https://sonarsource.atlassian.net/browse/SONARKT",
                "Plugin-Key" to "kotlin",
                "Plugin-License" to "SSALv1",
                "Plugin-Name" to "Kotlin Code Quality and Security",
                "Plugin-Organization" to "SonarSource",
                "Plugin-OrganizationUrl" to "https://www.sonarsource.com",
                "Plugin-SourcesUrl" to "https://github.com/SonarSource/sonar-kotlin",
                "Plugin-Version" to project.version,
                "Plugin-RequiredForLanguages" to "kotlin",
                "Sonar-Version" to "6.7",
                "SonarLint-Supported" to "true",
                "Version" to project.version.toString(),
                "Jre-Min-Version" to java.sourceCompatibility.majorVersion
            )
        )
    }
}

val sourcesJar = tasks.sourcesJar
val javadocJar = tasks.javadocJar

val patchTask = project(":sonar-kotlin-api").tasks.named("patchKotlinCompiler")

// Note that this task is time-consuming
// and needed only for integration tests and publishing,
// so it is not part of `gradle build`.
task<proguard.gradle.ProGuardTask>("dist") {
    group = "build"
    description = "Assembles sonar-kotlin-plugin.jar for integration tests and publishing"
    libraryjars("${System.getProperty("java.home")}/jmods/java.base.jmod")
    injars(
        mapOf(
            "filter" to listOf(
                "!META-INF/*.kotlin_module",
                "!org/jetbrains/kotlin/psi/KtVisitor.class", // patched version is included below
                "!com/intellij/util/concurrency/AppScheduledExecutorService\$MyThreadFactory.class", // patched version is included below
                "!META-INF/native/**/*jansi*",
                "!org/jline/**",
                "!net/jpountz/**"
            ).joinToString(",")
        ),
        tasks.shadowJar.get().archiveFile
    )
    injars(patchTask)
    outjars("build/libs/sonar-kotlin-plugin.jar")
    configuration("proguard.txt")
    doLast {
        enforceJarSizeAndCheckContent(file("build/libs/sonar-kotlin-plugin.jar"), 49_600_000L, 50_000_000L)
    }
}

tasks.artifactoryPublish { skip = false }
publishing {
    // gradle :sonar-kotlin-plugin:publishToMavenLocal
    publications.withType<MavenPublication> {
        artifact(tasks.named("dist")) {
            classifier = null
        }
        artifact(sourcesJar)
        artifact(javadocJar)
    }
}

fun enforceJarSizeAndCheckContent(file: File, minSize: Long, maxSize: Long) {
    val size = file.length()
    if (size < minSize) {
        throw GradleException("${file.path} size ($size) too small. Min is $minSize")
    } else if (size > maxSize) {
        throw GradleException("${file.path} size ($size) too large. Max is $maxSize")
    } else {
        logger.lifecycle("${file.path} size $size")
    }
    checkJarEntriesPathUniqueness(file)
}

// A jar should not contain 2 entries with the same path, furthermore Pack200 will fail to unpack it
fun checkJarEntriesPathUniqueness(file: File) {
    val allNames = mutableSetOf<String>()
    val duplicatedNames = mutableSetOf<String>()
    file.inputStream().use { input ->
        JarInputStream(input).use { jarInput ->
            generateSequence { jarInput.nextJarEntry }.forEach { jarEntry ->
                if (!allNames.add(jarEntry.name)) {
                    duplicatedNames.add(jarEntry.name)
                }
            }
        }
    }
    if (duplicatedNames.isNotEmpty()) {
        throw GradleException("Duplicated entries in the jar: '${file.path}': ${duplicatedNames.joinToString(", ")}")
    }
}

tasks.check {
    // Generate aggregate coverage report
    dependsOn(tasks.named<JacocoReport>("testCodeCoverageReport"))
}
