/*
 * SonarSource SLang
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
import org.sonarsource.kotlin.converter.KotlinTree
import org.sonarsource.kotlin.plugin.KotlinPlugin.Companion.KOTLIN_REPOSITORY_KEY
import org.sonarsource.kotlin.plugin.KotlinPlugin.Companion.SONAR_JAVA_BINARIES
import org.sonarsource.kotlin.plugin.KotlinPlugin.Companion.SONAR_JAVA_LIBRARIES
import org.sonarsource.kotlin.visiting.KotlinFileVisitor
import org.sonarsource.kotlin.visiting.KtChecksVisitor
import java.io.IOException
import java.util.concurrent.TimeUnit

private val LOG = Loggers.get(KotlinSensor::class.java)
private val EMPTY_FILE_CONTENT_PATTERN = Regex("""\s*+""")

class KotlinSensor(
    checkFactory: CheckFactory,
    private val fileLinesContextFactory: FileLinesContextFactory,
    private val noSonarFilter: NoSonarFilter,
    val language: KotlinLanguage,
) : Sensor {

    val checks: Checks<AbstractCheck> = checkFactory.create<AbstractCheck>(KOTLIN_REPOSITORY_KEY).apply {
        addAnnotatedChecks(KotlinCheckList.checks() as Iterable<*>)
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

        progressReport.start(filenames)
        var success = false

        try {
            success = analyseFiles(sensorContext, inputFiles, progressReport, visitors(sensorContext), statistics)
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
    ): Boolean {
        for (inputFile in inputFiles) {
            if (sensorContext.isCancelled) return false

            val inputFileContext = InputFileContextImpl(sensorContext, inputFile)
            try {
                analyseFile(sensorContext, inputFileContext, visitors, statistics)
            } catch (e: ParseException) {
                logParsingError(inputFile, e)
                inputFileContext.reportAnalysisParseError(KOTLIN_REPOSITORY_KEY, inputFile, e.position)
            }
            progressReport.nextFile()
        }
        return true
    }

    private fun analyseFile(
        sensorContext: SensorContext,
        inputFileContext: InputFileContext,
        visitors: List<KotlinFileVisitor>,
        statistics: DurationStatistics,
    ) {
        val inputFile = inputFileContext.inputFile
        val content = try {
            inputFile.contents()
        } catch (e: IOException) {
            throw toParseException("read", inputFile, e)
        } catch (e: RuntimeException) {
            throw toParseException("read", inputFile, e)
        }

        if (EMPTY_FILE_CONTENT_PATTERN.matches(content)) {
            return
        }

        val environment = environment(sensorContext)
        try {
            parseAndVisitFile(content, environment, inputFileContext, visitors, statistics)
        } finally {
            Disposer.dispose(environment.disposable)
        }
    }

    private fun parseAndVisitFile(
        content: String,
        environment: Environment,
        inputFileContext: InputFileContext,
        visitors: List<KotlinFileVisitor>,
        statistics: DurationStatistics,
    ) {
        val inputFile = inputFileContext.inputFile
        val tree = statistics.time<KotlinTree>("Parse") {
            try {
                KotlinTree.of(content, environment)
            } catch (e: RuntimeException) {
                throw toParseException("parse", inputFile, e)
            }
        }

        for (visitor in visitors) {
            try {
                val visitorId = visitor.javaClass.simpleName
                statistics.time(visitorId) { visitor.scan(inputFileContext, tree) }
            } catch (e: RuntimeException) {
                inputFileContext.reportAnalysisError(e.message, null)
                LOG.error("Cannot analyse '" + inputFile + "': " + e.message, e)
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
    ParseException("Cannot " + action + " '" + inputFile + "': " + cause.message, (cause as? ParseException)?.position, cause)

fun environment(sensorContext: SensorContext) = Environment(
    getFilesFromProperty(sensorContext.config(), SONAR_JAVA_BINARIES) +
        getFilesFromProperty(sensorContext.config(), SONAR_JAVA_LIBRARIES)
)
