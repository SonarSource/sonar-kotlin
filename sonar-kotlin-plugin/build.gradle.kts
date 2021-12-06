import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.jar.JarInputStream


plugins {
    id("com.github.johnrengelman.shadow") version "7.0.0"
    kotlin("jvm")
    id("com.diffplug.spotless") version "5.15.0"
}

configure<com.diffplug.gradle.spotless.SpotlessExtension> {

    lineEndings = com.diffplug.spotless.LineEnding.UNIX

    fun SourceSet.findSourceFilesToTarget() = allJava.srcDirs.flatMap { srcDir ->
        project.fileTree(srcDir).filter { file ->
            file.name.endsWith(".kt") || (file.name.endsWith(".java") && file.name != "package-info.java")
        }
    }

    kotlin {
        // ktlint()
        licenseHeaderFile(rootProject.file("LICENSE_HEADER")).updateYearWithLatest(true)

        target(
            project.sourceSets.main.get().findSourceFilesToTarget(),
            project.sourceSets.test.get().findSourceFilesToTarget()
        )
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }

    format("misc") {
        // define the files to apply `misc` to
        target("*.gradle", "*.md", ".gitignore")

        // define the steps to apply to those files
        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
    }
}

val kotlinVersion: String by extra
val junitVersion: String by project
val mockkVersion: String by project
val staxmateVersion: String by project

dependencies {
    compileOnly("org.sonarsource.sonarqube:sonar-plugin-api")
    implementation("org.sonarsource.analyzer-commons:sonar-analyzer-commons")
    implementation("org.sonarsource.analyzer-commons:sonar-xml-parsing")
    implementation("org.sonarsource.analyzer-commons:sonar-regex-parsing")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
    implementation("com.fasterxml.staxmate:staxmate:$staxmateVersion")
    implementation("com.google.code.gson:gson")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito:mockito-core")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.github.classgraph:classgraph")
    testImplementation("org.sonarsource.analyzer-commons:sonar-analyzer-test-commons")
    testImplementation("org.sonarsource.sonarqube:sonar-plugin-api-impl")
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
        enforceJarSizeAndCheckContent(shadowJar.get().archiveFile.get().asFile, 33_400_000L, 36_000_000L)
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
