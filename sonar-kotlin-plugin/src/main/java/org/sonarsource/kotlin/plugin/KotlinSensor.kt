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
package org.sonarsource.kotlin.plugin

import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.resolve.BindingContext
import org.sonar.api.SonarProduct
import org.sonar.api.batch.fs.FileSystem
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.rule.CheckFactory
import org.sonar.api.batch.rule.Checks
import org.sonar.api.batch.sensor.Sensor
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.SensorDescriptor
import org.sonar.api.issue.NoSonarFilter
import org.sonar.api.measures.FileLinesContextFactory
import org.sonar.api.utils.log.Loggers
import org.sonarsource.analyzer.commons.ProgressReport
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.InputFileContext
import org.sonarsource.kotlin.api.ParseException
import org.sonarsource.kotlin.api.regex.RegexCache
import org.sonarsource.kotlin.converter.Environment
import org.sonarsource.kotlin.converter.KotlinSyntaxStructure
import org.sonarsource.kotlin.converter.KotlinTree
import org.sonarsource.kotlin.converter.bindingContext
import org.sonarsource.kotlin.plugin.KotlinPlugin.Companion.COMPILER_THREAD_COUNT_PROPERTY
import org.sonarsource.kotlin.plugin.KotlinPlugin.Companion.DEFAULT_KOTLIN_LANGUAGE_VERSION
import org.sonarsource.kotlin.plugin.KotlinPlugin.Companion.FAIL_FAST_PROPERTY_NAME
import org.sonarsource.kotlin.plugin.KotlinPlugin.Companion.KOTLIN_LANGUAGE_VERSION
import org.sonarsource.kotlin.plugin.KotlinPlugin.Companion.KOTLIN_REPOSITORY_KEY
import org.sonarsource.kotlin.plugin.KotlinPlugin.Companion.PERFORMANCE_MEASURE_ACTIVATION_PROPERTY
import org.sonarsource.kotlin.plugin.KotlinPlugin.Companion.PERFORMANCE_MEASURE_DESTINATION_FILE
import org.sonarsource.kotlin.plugin.KotlinPlugin.Companion.SONAR_JAVA_BINARIES
import org.sonarsource.kotlin.plugin.KotlinPlugin.Companion.SONAR_JAVA_LIBRARIES
import org.sonarsource.kotlin.visiting.KotlinFileVisitor
import org.sonarsource.kotlin.visiting.KtChecksVisitor
import org.sonarsource.performance.measure.PerformanceMeasure
import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrElse
import kotlin.time.ExperimentalTime

private val LOG = Loggers.get(KotlinSensor::class.java)
private val EMPTY_FILE_CONTENT_PATTERN = Regex("""\s*+""")

@OptIn(ExperimentalTime::class)
class KotlinSensor(
    checkFactory: CheckFactory,
    private val fileLinesContextFactory: FileLinesContextFactory,
    private val noSonarFilter: NoSonarFilter,
    val language: KotlinLanguage,
) : Sensor {

    val checks: Checks<AbstractCheck> = checkFactory.create<AbstractCheck>(KOTLIN_REPOSITORY_KEY).apply {
        addAnnotatedChecks(KOTLIN_CHECKS as Iterable<*>)
        all().forEach { it.initialize(ruleKey(it)!!) }
    }

    override fun describe(descriptor: SensorDescriptor) {
        descriptor
            .onlyOnLanguage(language.key)
            .name(language.name + " Sensor")
    }

    override fun execute(sensorContext: SensorContext) {
        val sensorDuration = createPerformanceMeasureReport(sensorContext)

        val fileSystem: FileSystem = sensorContext.fileSystem()
        val mainFilePredicate = fileSystem.predicates().and(
            fileSystem.predicates().hasLanguage(language.key),
            fileSystem.predicates().hasType(InputFile.Type.MAIN)
        )

        val filesToAnalyze = fileSystem.inputFiles(mainFilePredicate).let { mainFiles ->
            if (canSkipUnchangedFiles(sensorContext)) {
                LOG.debug("The Kotlin analyzer is running in a context where it can skip unchanged files.")
                var totalFiles = 0
                mainFiles
                    .filter {
                        totalFiles++
                        it.status() != InputFile.Status.SAME
                    }.also {
                        LOG.info("Only analyzing ${it.size} changed Kotlin files out of ${totalFiles}.")
                    }
            } else {
                LOG.debug("The Kotlin analyzer is running in a context where unchanged files cannot be skipped.")
                mainFiles
            }
        }

        val filenames = filesToAnalyze.map { it.toString() }
        if (filenames.isEmpty()) {
            return
        }

        val progressReport = ProgressReport("Progress of the ${language.name} analysis", TimeUnit.SECONDS.toMillis(10))

        var success = false

        try {
            success = analyseFiles(sensorContext, filesToAnalyze, progressReport, visitors(sensorContext), filenames)
        } finally {
            if (success) {
                progressReport.stop()
            } else {
                progressReport.cancel()
            }
        }
        sensorDuration?.stop()
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun canSkipUnchangedFiles(sensorContext: SensorContext): Boolean {
        return sensorContext.config().getBoolean(KotlinPlugin.SKIP_UNCHANGED_FILES_OVERRIDE).getOrElse {
            try {
                sensorContext.canSkipUnchangedFiles()
            } catch (_: IncompatibleClassChangeError) {
                false
            }
        }
    }

    private fun analyseFiles(
        sensorContext: SensorContext,
        inputFiles: Iterable<InputFile>,
        progressReport: ProgressReport,
        visitors: List<KotlinFileVisitor>,
        filenames: List<String>,
    ): Boolean {
        val environment = environment(sensorContext)
        try {
            val isInAndroidContext = isInAndroidContext(environment)
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

            val bindingContext = runCatching {
                measureDuration("BindingContext") {
                    bindingContext(
                        environment.env,
                        environment.classpath,
                        kotlinFiles.map { it.ktFile },
                    )
                }
            }.getOrElse { e ->
                LOG.error("Could not generate binding context. Proceeding without semantics.", e)
                BindingContext.EMPTY
            }

            val diagnostics = measureDuration("Diagnostics") {
                bindingContext.diagnostics.noSuppression().groupBy { it.psiFile }.toMap()
            }

            val regexCache = RegexCache()

            progressReport.start(filenames)
            for ((ktFile, doc, inputFile) in kotlinFiles) {
                if (sensorContext.isCancelled) return false
                val inputFileContext = InputFileContextImpl(sensorContext, inputFile, isInAndroidContext)

                measureDuration(inputFile.filename()) {
                    analyseFile(
                        sensorContext,
                        inputFileContext,
                        visitors,
                        KotlinTree(ktFile, doc, bindingContext, diagnostics[ktFile] ?: emptyList(), regexCache),
                    )
                }

                progressReport.nextFile()
            }
        } finally {
            Disposer.dispose(environment.disposable)
        }
        return true
    }

    private fun analyseFile(
        sensorContext: SensorContext,
        inputFileContext: InputFileContext,
        visitors: List<KotlinFileVisitor>,
        tree: KotlinTree,
    ) {
        if (EMPTY_FILE_CONTENT_PATTERN.matches(inputFileContext.inputFile.contents())) {
            return
        }
        visitFile(sensorContext, inputFileContext, visitors, tree)
    }

    private fun visitFile(
        sensorContext: SensorContext,
        inputFileContext: InputFileContext,
        visitors: List<KotlinFileVisitor>,
        tree: KotlinTree,
    ) {
        for (visitor in visitors) {
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

    private fun visitors(sensorContext: SensorContext): List<KotlinFileVisitor> =
        if (sensorContext.runtime().product == SonarProduct.SONARLINT) {
            listOf(
                IssueSuppressionVisitor(),
                KtChecksVisitor(checks),
            )
        } else {
            listOf(
                IssueSuppressionVisitor(),
                MetricVisitor(fileLinesContextFactory, noSonarFilter),
                KtChecksVisitor(checks),
                CopyPasteDetector(),
                SyntaxHighlighter(),
            )
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
    determineKotlinLanguageVersion(sensorContext),
    numberOfThreads = determineNumberOfThreadsToUse(sensorContext)
)

private fun determineNumberOfThreadsToUse(sensorContext: SensorContext) =
    sensorContext.config().get(COMPILER_THREAD_COUNT_PROPERTY).map { stringInput ->
        runCatching {
            stringInput.trim().toInt()
        }.getOrElse {
            LOG.warn(
                "$COMPILER_THREAD_COUNT_PROPERTY needs to be set to an integer value. Could not interpret '$stringInput' as integer."
            )
            null
        }?.let { threadCount ->
            if (threadCount > 0) {
                threadCount
            } else {
                LOG.warn("Invalid amount of threads specified for $COMPILER_THREAD_COUNT_PROPERTY: '$stringInput'.")
                null
            }
        }
    }.orElse(null).also {
        LOG.debug("Using ${it ?: "the default amount of"} threads")
    }

private fun determineKotlinLanguageVersion(sensorContext: SensorContext) =
    (sensorContext.config().get(KOTLIN_LANGUAGE_VERSION).map { versionString ->
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
