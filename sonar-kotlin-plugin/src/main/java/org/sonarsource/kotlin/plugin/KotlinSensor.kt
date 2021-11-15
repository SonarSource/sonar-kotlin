/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2021 SonarSource SA
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
import org.sonar.api.SonarProduct
import org.sonar.api.batch.fs.FileSystem
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.rule.CheckFactory
import org.sonar.api.batch.rule.Checks
import org.sonar.api.batch.sensor.Sensor
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.SensorDescriptor
import org.sonar.api.config.Configuration
import org.sonar.api.issue.NoSonarFilter
import org.sonar.api.measures.FileLinesContextFactory
import org.sonar.api.utils.log.Loggers
import org.sonarsource.analyzer.commons.ProgressReport
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.InputFileContext
import org.sonarsource.kotlin.api.ParseException
import org.sonarsource.kotlin.converter.Environment
import org.sonarsource.kotlin.converter.KotlinSyntaxStructure
import org.sonarsource.kotlin.converter.KotlinTree
import org.sonarsource.kotlin.converter.bindingContext
import org.sonarsource.kotlin.plugin.KotlinPlugin.Companion.KOTLIN_REPOSITORY_KEY
import org.sonarsource.kotlin.plugin.KotlinPlugin.Companion.SONAR_JAVA_BINARIES
import org.sonarsource.kotlin.plugin.KotlinPlugin.Companion.SONAR_JAVA_LIBRARIES
import org.sonarsource.kotlin.visiting.KotlinFileVisitor
import org.sonarsource.kotlin.visiting.KtChecksVisitor
import java.util.concurrent.TimeUnit
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

@ExperimentalTime
private val LOG = Loggers.get(KotlinSensor::class.java)
private val EMPTY_FILE_CONTENT_PATTERN = Regex("""\s*+""")

@ExperimentalTime
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
        val statistics = DurationStatistics(sensorContext.config())

        val fileSystem: FileSystem = sensorContext.fileSystem()
        val mainFilePredicate = fileSystem.predicates().and(
            fileSystem.predicates().hasLanguage(language.key),
            fileSystem.predicates().hasType(InputFile.Type.MAIN))

        val inputFiles = fileSystem.inputFiles(mainFilePredicate)
        val filenames = inputFiles.map { it.toString() }

        val progressReport = ProgressReport("Progress of the ${language.name} analysis", TimeUnit.SECONDS.toMillis(10))

        var success = false

        try {
            success = analyseFiles(sensorContext, inputFiles, progressReport, visitors(sensorContext), statistics, filenames)
        } finally {
            if (success) {
                progressReport.stop()
            } else {
                progressReport.cancel()
            }
        }
        statistics.log()
    }

    private fun analyseFiles(
        sensorContext: SensorContext,
        inputFiles: Iterable<InputFile>,
        progressReport: ProgressReport,
        visitors: List<KotlinFileVisitor>,
        statistics: DurationStatistics,
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
            val (bindingContext, duration) = measureTimedValue {
                bindingContext(
                    environment.env,
                    environment.classpath,
                    kotlinFiles.map { it.ktFile },
                )
            }

            LOG.info("Generating BindingContext for all files took: ${duration.inWholeMilliseconds} ms.")

            progressReport.start(filenames)
            for ((ktFile, doc, inputFile) in kotlinFiles) {
                if (sensorContext.isCancelled) return false
                val inputFileContext = InputFileContextImpl(sensorContext, inputFile, isInAndroidContext)

                analyseFile(inputFileContext, visitors, statistics, KotlinTree(ktFile, doc, bindingContext))

                progressReport.nextFile()
            }
        } finally {
            Disposer.dispose(environment.disposable)
        }
        return true
    }

    private fun analyseFile(
        inputFileContext: InputFileContext,
        visitors: List<KotlinFileVisitor>,
        statistics: DurationStatistics,
        tree: KotlinTree,
    ) {
        if (EMPTY_FILE_CONTENT_PATTERN.matches(inputFileContext.inputFile.contents())) {
            return
        }
        visitFile(inputFileContext, visitors, statistics, tree)
    }

    private fun visitFile(
        inputFileContext: InputFileContext,
        visitors: List<KotlinFileVisitor>,
        statistics: DurationStatistics,
        tree: KotlinTree,
    ) {
        for (visitor in visitors) {
            try {
                val visitorId = visitor.javaClass.simpleName
                statistics.time(visitorId) { visitor.scan(inputFileContext, tree) }
            } catch (e: RuntimeException) {
                inputFileContext.reportAnalysisError(e.message, null)
                LOG.error("Cannot analyse '${inputFileContext.inputFile}': ${e.message}", e)
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

fun getFilesFromProperty(settings: Configuration, property: String): List<String> =
    settings.get(property).map {
        if (it.isNotBlank()) it.split(",").toList() else emptyList()
    }.orElse(emptyList())

private fun toParseException(action: String, inputFile: InputFile, cause: Throwable) =
    ParseException("Cannot $action '$inputFile': ${cause.message}", (cause as? ParseException)?.position, cause)

fun environment(sensorContext: SensorContext) = Environment(
    getFilesFromProperty(sensorContext.config(), SONAR_JAVA_BINARIES) +
        getFilesFromProperty(sensorContext.config(), SONAR_JAVA_LIBRARIES)
)
