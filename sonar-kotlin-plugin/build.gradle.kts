import com.google.gson.JsonParser
import de.undercouch.gradle.tasks.download.Download
import java.io.IOException
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.jar.JarInputStream
import org.sonarsource.cloudnative.gradle.licenseTitleToResourceFile
import proguard.gradle.ProGuardTask

plugins {
    id("com.gradleup.shadow") version "8.3.1"
    kotlin("jvm")
    id("jacoco-report-aggregation")
    id("org.sonarsource.cloud-native.license-file-generator")
    id("de.undercouch.download") version "5.7.0"
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

// Configuration to resolve kotlin-compiler dependency
val kotlinCompilerJar: Configuration = configurations.create("kotlinCompilerJar") {
    isCanBeConsumed = false
    isCanBeResolved = true
    extendsFrom(configurations.implementation.get())
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    }
}

val preprocessKotlinCompiler = tasks.register<Copy>("preprocessKotlinCompiler") {
    group = "build"
    description = "Before including kotlin-compiler into the shadow jar, filter out some files and verify that all licenses are accounted for"

    from(provider {
        val compilerJar = kotlinCompilerJar.resolvedConfiguration.resolvedArtifacts
            .find { it.moduleVersion.id.module.name == "kotlin-compiler" }
            ?.file
            ?: throw GradleException("kotlin-compiler dependency not found")

        zipTree(compilerJar)
    }) {
        exclude(
            // Files also excluded by ProGuard (see dist task)
            "META-INF/*.kotlin_module",
            "org/jetbrains/kotlin/psi/KtVisitor.class", // patched version is included separately
            "com/intellij/util/concurrency/AppScheduledExecutorService\$MyThreadFactory.class", // patched version is included separately
            "META-INF/native/**/*jansi*",
            "org/jline/**",
            "net/jpountz/**",

            // Additional exclusions
            "org/codehaus/stax2/**", // a stripped down version of the class breaks our usage, we include the full version ourselves
            "org/fusesource/jansi/**", // jansi dependency not used
            "META-INF/services/org/jline/**", // service provider files for jline
        )
    }

    into(layout.buildDirectory.dir("preprocessed/kotlin-compiler"))

    eachFile {
        val isKnownPackage = packagesToDependencies.keys.any { prefix ->
            path.startsWith(prefix)
        }

        if (!isKnownPackage) {
            throw GradleException("Unexpected package inside kotlin-compiler: $path. Please update the exclude list in preprocessKotlinCompiler task " +
                "or add a license for this dependency in packagesToDependencies map")
        }
    }
}

tasks.shadowJar {
    dependsOn(preprocessKotlinCompiler)

    dependencies {
        exclude(dependency("org.jetbrains.kotlin:kotlin-compiler:.*"))
    }

    from(preprocessKotlinCompiler.map { it.outputs.files })
}

// Note that this task is time-consuming
// and needed only for integration tests and publishing,
// so it is not part of `gradle build`.
tasks.register<ProGuardTask>("dist") {
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
        enforceJarSizeAndCheckContent(file("build/libs/sonar-kotlin-plugin.jar"), 50_400_000L, 50_800_000L)
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

val fetchKotlinLicenseFileList = tasks.register<Download>("fetchKotlinLicenseFileList") {
    group = "build"
    description = "Fetches the list of third-party license files from the Kotlin repository."

    src("https://api.github.com/repos/JetBrains/kotlin/contents/license/third_party")
    dest(layout.buildDirectory.file("tmp/kotlin-license-files.json"))
    overwrite(true)
}

val downloadKotlinCompilerThirdPartyLicenses = tasks.register<Download>("downloadKotlinCompilerThirdPartyLicenses") {
    group = "build"
    description = "Downloads the third-party license files used by the Kotlin compiler"

    dependsOn(fetchKotlinLicenseFileList, "generateLicenseResources")
    mustRunAfter("generateLicenseResources")

    val baseUrl = "https://raw.githubusercontent.com/JetBrains/kotlin/master/license/third_party"
    val fileListProvider = fetchKotlinLicenseFileList.map { task ->
        val jsonFile = task.dest as File
        val files = JsonParser.parseString(jsonFile.readText()).asJsonArray
            .map { it.asJsonObject.get("name").asString }
            .filter { it.endsWith(".txt") }
        files.map { "$baseUrl/$it" }
    }

    src(fileListProvider.map { it.filter {
        // Exclude files that are not bundled into kotlin-compiler.jar
        val baseName = it.substringAfterLast("/")
            .removeSuffix(".txt")
            .replace(Regex("(_license|_LICENSE|_licence|LICENSE)$"), "")
        getFqName(baseName) != null
    } })
    dest(layout.buildDirectory.dir("tmp/kotlin-licenses-raw"))
    onlyIfModified(true)

    doLast {
        copy {
            from(layout.buildDirectory.dir("tmp/kotlin-licenses-raw"))
            into(layout.projectDirectory.dir("src/main/resources/licenses/THIRD_PARTY_LICENSES"))
            // If there are overlapping dependencies, prefer licenses that are already present
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE

            rename { filename ->
                val baseName = filename
                    .removeSuffix(".txt")
                    .replace(Regex("(_license|_LICENSE|_licence|LICENSE)$"), "")
                val fqName = getFqName(baseName)
                "$fqName-LICENSE.txt"
            }
        }
    }
}

val renderKotlinCompilerThirdPartyLicenses = tasks.register<DefaultTask>("renderKotlinCompilerThirdPartyLicenses") {
    group = "build"
    description = "Generates license files for the third-party dependencies of the Kotlin compiler that are not already included in the resources"

    val generatedLicenseResourcesDirectory = layout.projectDirectory.dir("src/main/resources/licenses/THIRD_PARTY_LICENSES").asFile
    outputs.dir(generatedLicenseResourcesDirectory)

    doLast {
        generatedLicenseResourcesDirectory.mkdirs()

        dependencies
            .filter { it.value.licenseName != null }
            .forEach { (fqName, info) ->
                val licenseName = info.licenseName!!
                val licenseResourceFileName = licenseTitleToResourceFile[licenseName]
                    ?: throw GradleException("License '$licenseName' not found in licenseTitleToResourceFile map for dependency $fqName")

                // Parse Maven coordinates: "group:artifact"
                val parts = fqName.split(":")
                if (parts.size != 2) {
                    logger.warn("Skipping $fqName - not a valid Maven coordinate")
                    return@forEach
                }
                val (group, artifact) = parts

                val targetFile = File(generatedLicenseResourcesDirectory, "$group.$artifact-LICENSE.txt")

                if (targetFile.exists()) {
                    logger.info("License file already exists for $fqName at ${targetFile.path}, skipping generation")
                    return@forEach
                }

                val resourceStream = rootDir.resolve("build-logic/common/gradle-modules/src/main/resources/licenses/$licenseResourceFileName")
                    .takeIf { it.exists() }
                    ?: throw IOException("Resource not found: license file not found for $fqName with license '$licenseName' (expected resource file: $licenseResourceFileName)")

                resourceStream.copyTo(targetFile)

                logger.info("Generated license file: ${targetFile.name} for $fqName (license: $licenseName)")
            }
    }
}

tasks.named("generateLicenseResources") {
    finalizedBy(renderKotlinCompilerThirdPartyLicenses)
}

/**
 * @property shortName Dependency is referenced by this name in the kotlin repo.
 */
private data class DependencyInfo(
    val packages: List<String> = emptyList(),
    val shortName: String? = null,
    val licenseName: String? = null
) {
    init {
        require(shortName != null || licenseName != null) {
            "Set either a shortName or a licenseName for a dependency"
        }
    }
}

// Centralized mapping of all dependencies embedded in kotlin-compiler.jar
// Keys are fully qualified dependency names (Maven coordinates)
private val dependencies = mapOf(
    "com.fasterxml:aalto-xml" to DependencyInfo(
        packages = listOf("com/fasterxml/aalto"),
        shortName = "aalto_xml",
        licenseName = "Apache 2.0"
    ),
    "com.fasterxml.woodstox:woodstox-core" to DependencyInfo(
        packages = listOf("com/ctc"),
        shortName = null,
        licenseName = "Apache 2.0"
    ),
    "org.codehaus.woodstox:stax2-api" to DependencyInfo(
        packages = listOf("org/codehaus/stax2"),
        shortName = "stax2-api",
        licenseName = "BSD"
        // Excluded - we include full version ourselves
    ),
    "com.github.ben-manes.caffeine:caffeine" to DependencyInfo(
        packages = listOf("com/github/benmanes/caffeine"),
        shortName = "caffeine",
        licenseName = "Apache 2.0"
    ),
    "com.google.guava:guava" to DependencyInfo(
        packages = listOf("com/google/common"),
        shortName = "guava",
        licenseName = "Apache 2.0"
    ),
    "com.google.gwt:gwt-user" to DependencyInfo(
        packages = listOf("com/google/gwt"),
        shortName = "gwt",
        licenseName = "Apache 2.0"
    ),
    "com.sun.jna:jna" to DependencyInfo(
        packages = listOf("com/sun/jna"),
        shortName = "sun",
        licenseName = "Apache 2.0"
    ),
    "io.opentelemetry:opentelemetry-api" to DependencyInfo(
        packages = listOf("io/opentelemetry"),
        shortName = "opentelemetry",
        licenseName = "Apache 2.0"
    ),

    "io.vavr:vavr" to DependencyInfo(
        packages = listOf("io/vavr"),
        shortName = null,
        licenseName = "Apache 2.0"
    ),
    "it.unimi.dsi:fastutil" to DependencyInfo(
        packages = listOf("it/unimi/dsi"),
        shortName = null,
        licenseName = "Apache 2.0"
    ),
    "one.util.streamex:streamex" to DependencyInfo(
        packages = listOf("one/util/streamex"),
        shortName = null,
        licenseName = "Apache 2.0"
    ),
    "org.apache.logging.log4j:log4j-api" to DependencyInfo(
        packages = listOf("org/apache/log4j"),
        shortName = null,
        licenseName = "Apache 2.0"
    ),
    "org.jdom:jdom2" to DependencyInfo(
        packages = listOf("org/jdom"),
        shortName = null,
        licenseName = "BSD"
    ),
    "org.picocontainer:picocontainer" to DependencyInfo(
        packages = listOf("org/picocontainer"),
        shortName = null,
        licenseName = "BSD"
    ),

    // Standard Java APIs
    "javax.inject:javax.inject" to DependencyInfo(
        packages = listOf("javax/inject"),
        shortName = null,
        licenseName = "Apache 2.0"
    ),

    // Kotlin compiler and IntelliJ platform (Apache 2.0 license, bundled with kotlin-compiler)
    "org.jetbrains.kotlin:kotlin-compiler" to DependencyInfo(
        packages = listOf("com/intellij", "org/jetbrains"),
        shortName = null,
        licenseName = "Apache 2.0"
    ),
    "org.jetbrains.kotlin:kotlin-stdlib" to DependencyInfo(
        packages = listOf("kotlin/"),
        shortName = null,
        licenseName = "Apache 2.0"
    ),
    "org.jetbrains.kotlinx:kotlinx-coroutines-core" to DependencyInfo(
        packages = listOf("kotlinx/"),
        shortName = null,
        licenseName = "Apache 2.0"
    ),

    // Resources and metadata (not code dependencies)
    "resources" to DependencyInfo(
        packages = listOf("META-INF/", "messages/", "misc/", "custom-formatters.js", "kotlinManifest.properties"),
        shortName = "resources",
        licenseName = null // N/A for resources
    ),
)

private val packagesToDependencies: Map<String, String> by lazy {
    dependencies
        .flatMap { (fqName, info) ->
            info.packages.map { pkg -> pkg to fqName }
        }
        .toMap()
}

private fun getFqName(baseName: String): String? {
    if (baseName in listOf(
        "gradle_custom_user_plugin",
        "antlr_js_grammar", "assemblyscript", "closure-compiler",
        "jquery", "jshashtable", "karma", "karma-teamcity-reporter", "karma-teamcity", "lodash",
        "mocha-teamcity-reporter", "mocha-teamcity", "power_assert",
        "boost", "dart",
        "sl4f" // Typo in license file name, should be "slf4j"
    )) {
        logger.info("Artifact <$baseName> is not bundled into kotlin-compiler.jar")
        return null
    }

    val entry = dependencies.entries.find { it.value.shortName == baseName }
        ?: error("Unknown license file base name: $baseName. Please update the dependencies map.")

    return entry.key
}