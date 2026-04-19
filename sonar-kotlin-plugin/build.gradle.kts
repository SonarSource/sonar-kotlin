import org.sonarsource.kotlin.buildsrc.utils.kotlinCompilerDependencies
import org.sonarsource.kotlin.buildsrc.utils.packagesToDependencies
import proguard.gradle.ProGuardTask
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.jar.JarInputStream

plugins {
    id("com.gradleup.shadow") version "8.3.10"
    kotlin("jvm")
    id("jacoco-report-aggregation")
    id("org.sonarsource.cloud-native.license-file-generator")
    id("org.sonarsource.cloud-native.rule-api")
}

ruleApi {
    languageToSonarpediaDirectory.put("Kotlin", ".")
}

buildscript {
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.9.1")
    }
}

val kotlinCompilerEmbedded: Configuration by configurations.creating {
    isTransitive = false
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

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation(testLibs.junit.jupiter)
    testImplementation(libs.slf4j.api)
    testImplementation(testLibs.assertj.core)
    testImplementation(testLibs.mockito.core)
    testImplementation(testLibs.mockk)
    testImplementation(testLibs.sonar.analyzer.test.commons)
    testImplementation(testLibs.sonar.plugin.api.impl)
    testImplementation(testLibs.sonar.plugin.api.test.fixtures)

    testImplementation(project(":sonar-kotlin-test-api"))
    jacocoAggregation(project(":sonar-kotlin-test-api"))

    // replicate dependencies embedded into kotlin-compiler for easier license management
    kotlinCompilerDependencies.forEach {
        kotlinCompilerEmbedded("${it.fqName}:${it.version}")
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

// Configuration to resolve kotlin-compiler dependency
val kotlinCompilerJar: Configuration = configurations.create("kotlinCompilerJar") {
    isCanBeConsumed = false
    isCanBeResolved = true
    extendsFrom(configurations.implementation.get())
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    }
}

/**
 * A task to unpack kotlin-compiler.jar, process it, and feed into the shadowJar task instead of the normal jar.
 * Processing involves analyzing embedded dependencies and validating that we include all the licenses,
 * as well as excluding some files that we don't need and that cause issues if included.
 */
val preprocessKotlinCompiler = tasks.register<Copy>("preprocessKotlinCompiler") {
    group = "build"
    description = "Before including kotlin-compiler into the shadow jar, filter out some files and verify that all licenses are accounted for"

    val excludedPackages = setOf(
        // Packages also excluded by ProGuard (see dist task)
        "org/jline",
        "net/jpountz",

        "org/codehaus/stax2", // a stripped down version of the class breaks our usage, we include the full version ourselves
        "org/fusesource/jansi", // jansi dependency not used
        "org/apache/log4j", // everything should be using slf4j, we don't need to bundle a logging implementation
        "javax/inject" // a compile-time dependency
    )

    from(
        provider {
            val compilerJar = kotlinCompilerJar.resolvedConfiguration.resolvedArtifacts
                .find { it.moduleVersion.id.module.name == "kotlin-compiler" }
                ?.file
                ?: throw GradleException("kotlin-compiler dependency not found")

            zipTree(compilerJar)
        }
    ) {
        exclude(
            "META-INF/*.kotlin_module",
            "org/jetbrains/kotlin/psi/KtVisitor.class", // patched version is included separately
            "com/intellij/util/concurrency/AppScheduledExecutorService\$MyThreadFactory.class", // patched version is included separately
            "META-INF/native/**/*jansi*",
            "pluginsCompatibleWithK2Mode.txt", // a text file that we don't use

            "META-INF/services/org/jline", // service provider files for jline
            *excludedPackages.map { "$it/**" }.toTypedArray()
        )
    }

    into(layout.buildDirectory.dir("preprocessed/kotlin-compiler"))

    val isPackageVisited = kotlinCompilerDependencies.associate { it.fqName to false }
        .toMutableMap()
    eachFile {
        val knownPackage = packagesToDependencies.filter { (prefix, _) ->
            path.startsWith(prefix)
        }.values.firstOrNull()

        if (knownPackage == null) {
            throw GradleException("Unexpected package inside kotlin-compiler: $path. Please update the mapping to include a license for this dependency")
        } else {
            isPackageVisited[knownPackage] = true
        }
    }

    doLast {
        val packagesNotVisited = isPackageVisited
            .filterNot { (packageName, _) -> excludedPackages.any { path -> packageName.startsWith(path) } }
            .filterValues { !it }
            .keys
            .joinToString(", ")

        if (packagesNotVisited.isNotEmpty()) {
            throw GradleException(
                "Some expected packages were not found in kotlin-compiler: $packagesNotVisited. " +
                    "Please check if the kotlin-compiler dependency has changed and exclude any old dependencies that are no longer present"
            )
        }
    }
}

tasks.shadowJar {
    dependsOn(preprocessKotlinCompiler)

    dependencies {
        exclude(dependency(libs.kotlin.compiler))
    }

    from(preprocessKotlinCompiler.map { it.outputs.files })
}

// Note that this task is time-consuming
// and needed only for integration tests and publishing,
// so it is not part of `gradle build`.
val distTask = tasks.register<ProGuardTask>("dist") {
    group = "build"
    description = "Assembles sonar-kotlin-plugin.jar for integration tests and publishing"
    libraryjars("${System.getProperty("java.home")}/jmods/java.base.jmod")
    injars(tasks.shadowJar.get().archiveFile)
    outjars("build/libs/sonar-kotlin-plugin.jar")
    configuration("proguard.txt")
    doLast {
        enforceJarSizeAndCheckContent(file("build/libs/sonar-kotlin-plugin.jar"), 53_500_000L, 54_000_000L)
    }
}

val dist: Configuration by configurations.creating {
    isCanBeConsumed = true
    isCanBeResolved = false
    isTransitive = false
    attributes {
        attribute(Attribute.of("org.sonarsource.kotlin.dist", String::class.java), "sonar-kotlin-plugin")
    }
}
artifacts.add(dist.name, file("build/libs/sonar-kotlin-plugin.jar")) {
    builtBy(distTask)
}

tasks.artifactoryPublish { skip = false }
publishing {
    // gradle :sonar-kotlin-plugin:publishToMavenLocal
    publications.withType<MavenPublication> {
        artifact(distTask) {
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

licenseReport {
    configurations = arrayOf(
        kotlinCompilerEmbedded.name,
        "runtimeClasspath"
    )
}
