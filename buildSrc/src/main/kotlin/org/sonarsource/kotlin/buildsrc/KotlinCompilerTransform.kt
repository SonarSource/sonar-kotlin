package org.sonarsource.kotlin.buildsrc

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.provider.ProviderFactory

val packageExclusions = setOf(
    // Packages also excluded by ProGuard (see dist task in sonar-kotlin-plugin)
    "org/jline",
    "net/jpountz",

    "org/codehaus/stax2", // a stripped down version of the class breaks our usage, we include the full version ourselves
    "org/fusesource/jansi", // jansi dependency not used
    "org/apache/log4j", // everything should be using slf4j, we don't need to bundle a logging implementation
    "javax/inject" // a compile-time dependency
)

/**
 * Artifact Transform to remove unnecessary files from kotlin-compiler JAR.
 */
abstract class KotlinCompilerTransform @Inject constructor(private val providerFactory: ProviderFactory)
    : TransformAction<TransformParameters.None> {

    private val logger: Logger = Logging.getLogger(KotlinCompilerTransform::class.java)

    @get:InputArtifact
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        val kotlinVersion = providerFactory.gradleProperty("kotlinVersion").orNull
            ?: throw GradleException("kotlinVersion property not found")

        if (inputArtifact.get().asFile.name != "kotlin-compiler-$kotlinVersion.jar") {
            outputs.file(inputArtifact.get().asFile) // Pass through unmodified
            return
        }

        logger.info("KotlinCompilerTransform: Transforming ${inputArtifact.get().asFile.name}")

        val input = inputArtifact.get().asFile
        val output = outputs.file(input.name.replace(".jar", "-transformed.jar"))

        filterJar(input, output)
    }

    private fun filterJar(input: File, output: File) {
        var totalEntries = 0
        var filteredEntries = 0

        ZipFile(input).use { zipFile ->
            ZipOutputStream(output.outputStream().buffered()).use { zos ->
                zipFile.entries().asSequence().forEach { entry ->
                    totalEntries++
                    if (!shouldExclude(entry.name)) {
                        zipFile.getInputStream(entry).use { inputStream ->
                            val newEntry = ZipEntry(entry.name).apply {
                                time = entry.time
                                comment = entry.comment
                            }
                            zos.putNextEntry(newEntry)
                            inputStream.copyTo(zos)
                            zos.closeEntry()
                        }
                    } else {
                        filteredEntries++
                    }
                }
            }
        }

        logger.info("KotlinCompilerTransform: Filtered $filteredEntries/$totalEntries entries from ${input.name}")
    }

    private fun shouldExclude(path: String): Boolean {
        if (packageExclusions.any { path.startsWith(it) }) {
            logger.debug("KotlinCompilerTransform: Excluding $path due to package exclusion")
            return true
        }

        return false
    }
}
