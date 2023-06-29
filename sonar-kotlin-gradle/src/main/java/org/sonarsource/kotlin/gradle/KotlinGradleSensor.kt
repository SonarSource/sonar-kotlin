/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.kotlin.gradle

import org.gradle.tooling.GradleConnector
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptModel
import org.gradle.tooling.model.kotlin.dsl.KotlinDslScriptsModel
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.LanguageVersion
import org.slf4j.LoggerFactory
import org.sonar.api.batch.fs.FileSystem
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.sensor.Sensor
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.SensorDescriptor
import org.sonar.api.batch.ScannerSide
import org.sonar.api.batch.rule.CheckFactory
import org.sonar.api.batch.rule.Checks
import org.sonarsource.analyzer.commons.ProgressReport
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.InputFileContext
import org.sonarsource.kotlin.api.checks.InputFileContextImpl
import org.sonarsource.kotlin.api.common.DEFAULT_KOTLIN_LANGUAGE_VERSION
import org.sonarsource.kotlin.api.common.FAIL_FAST_PROPERTY_NAME
import org.sonarsource.kotlin.api.common.KOTLIN_LANGUAGE_VERSION
import org.sonarsource.kotlin.api.common.KOTLIN_REPOSITORY_KEY
import org.sonarsource.kotlin.api.common.KotlinLanguage
import org.sonarsource.kotlin.api.common.SONAR_ANDROID_DETECTED
import org.sonarsource.kotlin.api.common.SONAR_JAVA_BINARIES
import org.sonarsource.kotlin.api.common.SONAR_JAVA_LIBRARIES
import org.sonarsource.kotlin.api.common.measureDuration
import org.sonarsource.kotlin.api.frontend.Environment
import org.sonarsource.kotlin.api.frontend.KotlinSyntaxStructure
import org.sonarsource.kotlin.api.frontend.KotlinTree
import org.sonarsource.kotlin.api.frontend.ParseException
import org.sonarsource.kotlin.api.frontend.RegexCache
import org.sonarsource.kotlin.api.frontend.bindingContext
import org.sonarsource.kotlin.api.logging.debug
import org.sonarsource.kotlin.api.visiting.KotlinFileVisitor
import org.sonarsource.kotlin.visiting.KtChecksVisitor
import org.sonarsource.performance.measure.PerformanceMeasure
import java.io.File

import java.util.concurrent.TimeUnit

private val LOG = LoggerFactory.getLogger(KotlinGradleSensor::class.java)
private val EMPTY_FILE_CONTENT_PATTERN = Regex("""\s*+""")

@ScannerSide
class KotlinGradleSensor(
    checkFactory: CheckFactory,
    val language: KotlinLanguage,
) : Sensor {

    val checks: Checks<AbstractCheck> = checkFactory.create<AbstractCheck>(KOTLIN_REPOSITORY_KEY).apply {
        addAnnotatedChecks(KOTLIN_GRADLE_CHECKS as Iterable<*>)
        all().forEach { it.initialize(ruleKey(it)!!) }
    }

    override fun describe(descriptor: SensorDescriptor) {
        descriptor
            .onlyOnLanguage(language.key)
            .name("Gradle Sensor")
    }

    override fun execute(sensorContext: SensorContext) {
        val sensorDuration = createPerformanceMeasureReport(sensorContext)
        val fileSystem: FileSystem = sensorContext.fileSystem()

        val projectConnection = GradleConnector.newConnector()
            .forProjectDirectory(fileSystem.baseDir())
            .connect()

        projectConnection.newBuild()
            .forTasks("prepareKotlinBuildScriptModel")
            .run()

        val models = projectConnection.getModel(KotlinDslScriptsModel::class.java).scriptModels

        val filesToAnalyze: Iterable<InputFile> = models.keys.map { file ->
            val predicate = fileSystem.predicates().hasAbsolutePath(file.absolutePath)
            fileSystem.inputFile(predicate)
        }.filterNotNull()


        val fileNames = filesToAnalyze.map { it.toString() }
        if (fileNames.isEmpty()) return
        val progressReport = ProgressReport("Progress of the ${language.name} analysis", TimeUnit.SECONDS.toMillis(10))

        var success = false
        try {
            success = analyzeFiles(
                sensorContext,
                filesToAnalyze,
                progressReport,
                listOf(KtChecksVisitor(checks)),
                fileNames,
                models,
            )
        } finally {
            if (success) {
                progressReport.stop()
            } else {
                progressReport.cancel()
            }
        }
        sensorDuration?.stop()
    }

    private fun analyzeFiles(
        sensorContext: SensorContext,
        inputFiles: Iterable<InputFile>,
        progressReport: ProgressReport,
        visitors: List<KotlinFileVisitor>,
        fileNames: List<String>,
        models: MutableMap<File, KotlinDslScriptModel>,
    ): Boolean {
        val environment = environment(sensorContext)

        try {


            val classPath = models.values.flatMap { it.classPath.map { file -> file.absolutePath } }

            val isInAndroidContext = sensorContext.config().getBoolean(SONAR_ANDROID_DETECTED).orElse(false)
            val kotlinFiles = inputFiles.mapNotNull {
                val inputFileContext = InputFileContextImpl(sensorContext, it, isInAndroidContext)
                try {
                    KotlinSyntaxStructure.of(it.contents(), environment, it)
                } catch (e: ParseException) {
                    logParsingError(it, toParseException("parse", it, e))
                    inputFileContext.reportAnalysisParseError(KOTLIN_REPOSITORY_KEY, it, e.position)
                    null
                } catch (e: Exception) {
                    val parseException = toParseException("read", it, e)
                    logParsingError(it, parseException)
                    inputFileContext.reportAnalysisParseError(KOTLIN_REPOSITORY_KEY, it, parseException.position)
                    null
                }
            }

            val regexCache = RegexCache()

            val bindingContext = bindingContext(environment.env, classPath, kotlinFiles.map { it.ktFile })

            progressReport.start(fileNames)
            kotlinFiles.filter {
                !EMPTY_FILE_CONTENT_PATTERN.matches(it.inputFile.contents())
            }.forEach { (ktFile, doc, inputFile) ->
                if (sensorContext.isCancelled) return false
                val inputFileContext = InputFileContextImpl(sensorContext, inputFile, isInAndroidContext)
                val tree = KotlinTree(ktFile, doc, bindingContext, emptyList(), regexCache)

                measureDuration(inputFile.filename()) {
                    analyzeFile(sensorContext, inputFileContext, visitors, tree)
                }
                progressReport.nextFile()
            }
        } finally {
            Disposer.dispose(environment.disposable)
        }
        return true
    }

    private fun analyzeFile(
        sensorContext: SensorContext,
        inputFileContext: InputFileContext,
        visitors: List<KotlinFileVisitor>,
        tree: KotlinTree,
    ) {
        visitors.forEach { visitor ->
            val visitorId = visitor.javaClass.simpleName
            try {
                measureDuration(visitorId) {
                    visitor.scan(inputFileContext, tree)
                }
            } catch (e: Exception) {
                inputFileContext.reportAnalysisError(e.message, null)
                LOG.error("Cannot analyse '${inputFileContext.inputFile}' with '$visitorId': ${e.message}", e)
                if (sensorContext.config().getBoolean(FAIL_FAST_PROPERTY_NAME).orElse(false)) {
                    throw IllegalStateException(
                        "Exception in '$visitorId' while analyzing '${inputFileContext.inputFile}'",
                        e
                    )
                }
            }
        }
    }

    private fun logParsingError(inputFile: InputFile, e: ParseException) {
        val position = e.position
        var positionMessage = ""
        if (position != null) {
            positionMessage = "Parse error at position ${position.line()}:${position.lineOffset()}"
        }
        LOG.error("Unable to parse file: ${inputFile.uri()}. $positionMessage")
        e.message?.let { LOG.error(it) }
    }
}

private fun toParseException(action: String, inputFile: InputFile, cause: Throwable) =
    ParseException("Cannot $action '$inputFile': ${cause.message}", (cause as? ParseException)?.position, cause)

fun environment(sensorContext: SensorContext) = Environment(
    sensorContext.config().getStringArray(SONAR_JAVA_BINARIES).toList() +
            sensorContext.config().getStringArray(SONAR_JAVA_LIBRARIES).toList(),
    determineKotlinLanguageVersion(sensorContext)
)

private fun determineKotlinLanguageVersion(sensorContext: SensorContext) =
    (sensorContext.config()[KOTLIN_LANGUAGE_VERSION].map { versionString ->
        LanguageVersion.fromVersionString(versionString).also { langVersion ->
            if (langVersion == null && versionString.isNotBlank()) {
                LOG.warn("Failed to find Kotlin version '$versionString'. Defaulting to ${DEFAULT_KOTLIN_LANGUAGE_VERSION.versionString}")
            }
        }
    }.orElse(null) ?: DEFAULT_KOTLIN_LANGUAGE_VERSION)
        .also { LOG.debug { "Using Kotlin ${it.versionString} to parse source code" } }


private fun createPerformanceMeasureReport(context: SensorContext): PerformanceMeasure.Duration? {
    return PerformanceMeasure.reportBuilder()
        .activate(context.config()[PERFORMANCE_MEASURE_ACTIVATION_PROPERTY].filter { "true" == it }.isPresent)
        .toFile(context.config()[PERFORMANCE_MEASURE_DESTINATION_FILE].orElse("/tmp/sonar.kotlin.performance.measure.json"))
        .appendMeasurementCost()
        .start("KotlinSensor")
}

const val PERFORMANCE_MEASURE_ACTIVATION_PROPERTY = "sonar.kotlin.performance.measure"
const val PERFORMANCE_MEASURE_DESTINATION_FILE = "sonar.kotlin.performance.measure.json"
