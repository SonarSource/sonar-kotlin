import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.jar.JarInputStream


plugins {
    id("com.github.johnrengelman.shadow") version "7.1.0"
    kotlin("jvm")
}

dependencies {
    compileOnly(libs.sonar.plugin.api)
    implementation(libs.sonar.analyzer.commons)
    implementation(libs.sonar.xml.parsing)
    implementation(libs.sonar.regex.parsing)
    implementation(libs.sonar.performance.measure)
    implementation(libs.kotlin.compiler.embeddable)
    implementation(libs.staxmate)
    implementation(libs.gson)
    implementation(libs.sonar.analyzer.commons.recognizers)

    testImplementation(testLibs.junit.api)
    testImplementation(testLibs.junit.params)
    testRuntimeOnly(testLibs.junit.engine)
    testImplementation(testLibs.assertj.core)
    testImplementation(testLibs.mockito.core)
    testImplementation(testLibs.mockk)
    testImplementation(testLibs.classgraph)
    testImplementation(testLibs.sonar.analyzer.test.commons)
    testImplementation(testLibs.sonar.plugin.api.impl)
}

tasks.withType<JavaCompile> {
    // Prevent warning: Gradle 5.0 will ignore annotation processors
    options.compilerArgs = options.compilerArgs + "-proc:none"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
}

// The new version 11.0.17 of javadoc has a bug and does not handle package annotations correctly
// Adding a "tag" option is a workaround to prevent javadoc errors
// @see https://bugs.openjdk.org/browse/JDK-8295850
tasks.withType<Javadoc> {
    options {
        this as StandardJavadocDocletOptions
        addStringOption("tag", "javax.annotation.ParametersAreNonnullByDefault:ParametersAreNonnullByDefault")
    }
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
                "Plugin-License" to "GNU LGPL 3",
                "Plugin-Name" to "Kotlin Code Quality and Security",
                "Plugin-Organization" to "SonarSource",
                "Plugin-OrganizationUrl" to "https://www.sonarsource.com",
                "Plugin-SourcesUrl" to "https://github.com/SonarSource/sonar-kotlin",
                "Plugin-Version" to project.version,
                "Sonar-Version" to "6.7",
                "SonarLint-Supported" to "true",
                "Version" to project.version.toString()
            )
        )
    }
}

val shadowJar = tasks.shadowJar
val sourcesJar = tasks.sourcesJar
val javadocJar = tasks.javadocJar

tasks.shadowJar {
    minimize {}
    exclude("META-INF/native/**/*jansi*")
    exclude("org/jetbrains/kotlin/org/jline/**")
    exclude("org/jetbrains/kotlin/net/jpountz/**")
    doLast {
        enforceJarSizeAndCheckContent(shadowJar.get().archiveFile.get().asFile, 33_400_000L, 37_000_000L)
    }
}

artifacts {
    archives(shadowJar)
}

tasks.artifactoryPublish { skip = false }
publishing {
    publications.withType<MavenPublication> {
        artifact(shadowJar) {
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
