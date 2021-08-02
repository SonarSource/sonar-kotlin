import org.apache.groovy.dateutil.extensions.DateUtilExtensions
import java.util.Date
import java.util.jar.JarInputStream
import java.time.format.DateTimeFormatter
import java.time.ZoneId


plugins {
    id("com.github.johnrengelman.shadow") version "7.0.0"
    kotlin("jvm")
}

val kotlinVersion: String by extra

dependencies {
    compileOnly("org.sonarsource.sonarqube:sonar-plugin-api")
    implementation("org.sonarsource.analyzer-commons:sonar-analyzer-commons")
    implementation("org.sonarsource.analyzer-commons:sonar-xml-parsing")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
    implementation("com.fasterxml.staxmate:staxmate:2.3.1")
    implementation("com.eclipsesource.minimal-json:minimal-json:0.9.5")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.junit.jupiter:junit-jupiter-migrationsupport")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito:mockito-core")
    testImplementation("io.mockk:mockk:1.10.6")
    testImplementation("io.github.classgraph:classgraph")
    testImplementation("org.sonarsource.analyzer-commons:sonar-analyzer-test-commons")
}

tasks.withType<JavaCompile> {
    // Prevent warning: Gradle 5.0 will ignore annotation processors
    options.compilerArgs = options.compilerArgs + "-proc:none"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
}

val test: Test by tasks
test.dependsOn(project(":kotlin-checks-test-sources").tasks.named("build"))

tasks.jar {
    manifest {
        val displayVersion = if (project.property("buildNumber") == null) project.version else project.version.toString()
            .substring(0, project.version.toString().lastIndexOf(".")) + " (build ${project.property("buildNumber")})"
        val buildDate = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ").withZone(ZoneId.systemDefault()).format(Date().toInstant())
        attributes(mapOf(
            "Build-Time" to buildDate,
            "Implementation-Build" to ProcessBuilder().also { it.command("git", "rev-parse", "HEAD") }.start().inputStream.bufferedReader().use {
                it.readText().trim()
            },
            "Plugin-BuildDate" to buildDate,
            // Note that use of ChildFirstClassLoader is deprecated since SonarQube 7.9
            "Plugin-ChildFirstClassLoader" to "false",
            "Plugin-Class" to "org.sonarsource.kotlin.plugin.KotlinPlugin",
            "Plugin-Description" to "Code Analyzer for Kotlin",
            "Plugin-Developers" to "SonarSource Team",
            "Plugin-Display-Version" to displayVersion,
            "Plugin-Homepage" to "http to//redirect.sonarsource.com/plugins/kotlin.html",
            "Plugin-IssueTrackerUrl" to "https to//jira.sonarsource.com/browse/SONARKT",
            "Plugin-Key" to "kotlin",
            "Plugin-License" to "GNU LGPL 3",
            "Plugin-Name" to "Kotlin Code Quality and Security",
            "Plugin-Organization" to "SonarSource",
            "Plugin-OrganizationUrl" to "http to//www.sonarsource.com",
            "Plugin-SourcesUrl" to "https to//github.com/SonarSource/sonar-kotlin",
            "Plugin-Version" to project.version,
            "Sonar-Version" to "6.7",
            "SonarLint-Supported" to "true",
            "Version" to project.version.toString()
        ))
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
        enforceJarSizeAndCheckContent(shadowJar.get().archiveFile.get().asFile, 32_000_000L, 33_000_000L)
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
