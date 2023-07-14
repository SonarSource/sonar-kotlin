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

import org.jetbrains.kotlin.resolve.BindingContext
import org.slf4j.LoggerFactory
import org.sonar.api.SonarProduct
import org.sonar.api.batch.fs.FileSystem
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.rule.CheckFactory
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.batch.sensor.SensorDescriptor
import org.sonar.api.issue.NoSonarFilter
import org.sonar.api.measures.FileLinesContextFactory
import org.sonarsource.analyzer.commons.ProgressReport
import org.sonarsource.kotlin.api.checks.hasCacheEnabled
import org.sonarsource.kotlin.api.common.KotlinLanguage
import org.sonarsource.kotlin.api.common.measureDuration
import org.sonarsource.kotlin.api.frontend.bindingContext
import org.sonarsource.kotlin.api.logging.trace
import org.sonarsource.kotlin.api.sensors.AbstractKotlinSensor
import org.sonarsource.kotlin.api.sensors.AbstractKotlinSensorExecuteContext
import org.sonarsource.kotlin.plugin.caching.ContentHashCache
import org.sonarsource.kotlin.plugin.cpd.CopyPasteDetector
import org.sonarsource.kotlin.plugin.cpd.copyCPDTokensFromPrevious
import org.sonarsource.kotlin.plugin.cpd.loadCPDTokens
import org.sonarsource.kotlin.api.visiting.KotlinFileVisitor
import org.sonarsource.kotlin.metrics.IssueSuppressionVisitor
import org.sonarsource.kotlin.metrics.MetricVisitor
import org.sonarsource.kotlin.metrics.SyntaxHighlighter
import org.sonarsource.kotlin.visiting.KtChecksVisitor

import kotlin.jvm.optionals.getOrElse

private val LOG = LoggerFactory.getLogger(KotlinSensor::class.java)

private const val KOTLIN_SCRIPT_FILE_EXTENSIONS = ".kts"

class KotlinSensor(
    checkFactory: CheckFactory,
    private val fileLinesContextFactory: FileLinesContextFactory,
    private val noSonarFilter: NoSonarFilter,
    language: KotlinLanguage
): AbstractKotlinSensor(
    checkFactory, language, KOTLIN_CHECKS
) {
    override fun describe(descriptor: SensorDescriptor) {
        descriptor
            .onlyOnLanguage(language.key)
            .name(language.name + " Sensor")
    }

    override fun getExecuteContext(
        sensorContext: SensorContext,
        filesToAnalyze: Iterable<InputFile>,
        progressReport: ProgressReport,
        filenames: List<String>
    ) = object : AbstractKotlinSensorExecuteContext(
        sensorContext, filesToAnalyze, progressReport, visitors(sensorContext), filenames, LOG
    ) {
        override val bindingContext: BindingContext by lazy {
            runCatching {
                measureDuration("BindingContext") {
                    bindingContext(
                        environment.env,
                        environment.classpath,
                        kotlinFiles.map { it.ktFile }.filter { !it.isScript() },
                    )
                }
            }.getOrElse { e ->
                LOG.error("Could not generate binding context. Proceeding without semantics.", e)
                BindingContext.EMPTY
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

    override fun getFilesToAnalyse(sensorContext: SensorContext): Iterable<InputFile> {
        val fileSystem: FileSystem = sensorContext.fileSystem()
        val mainFilePredicate = fileSystem.predicates().and(
            fileSystem.predicates().hasLanguage(language.key),
            fileSystem.predicates().hasType(InputFile.Type.MAIN),
            fileSystem.predicates().not(fileSystem.predicates().hasExtension(KOTLIN_SCRIPT_FILE_EXTENSIONS))
        )

        return fileSystem.inputFiles(mainFilePredicate).let { mainFiles ->
            if (canSkipUnchangedFiles(sensorContext) && sensorContext.runtime().product != SonarProduct.SONARLINT) {
                val contentHashCache = ContentHashCache.of(sensorContext)
                LOG.debug("The Kotlin analyzer is running in a context where it can skip unchanged files.")
                var totalFiles = 0
                mainFiles.filter {
                    totalFiles++
                    fileHasChanged(it, contentHashCache) || !reuseCPDTokens(it, sensorContext)
                }.also {
                    LOG.info("Only analyzing ${it.size} changed Kotlin files out of ${totalFiles}.")
                }
            } else {
                LOG.debug("The Kotlin analyzer is running in a context where unchanged files cannot be skipped.")
                mainFiles
            }
        }
    }

    private fun reuseCPDTokens(inputFile: InputFile, sensorContext: SensorContext): Boolean {
        if (!sensorContext.hasCacheEnabled()) {
            return false
        }
        val previousCache = sensorContext.previousCache()
        return previousCache.loadCPDTokens(inputFile)?.let { previousTokens ->
            sensorContext.newCpdTokens().onFile(inputFile).apply {
                previousTokens.forEach { addToken(it.range, it.text) }
                save()
            }
            val nextCache = sensorContext.nextCache()
            try {
                nextCache.copyCPDTokensFromPrevious(inputFile)
            } catch (_: IllegalArgumentException) {
                LOG.trace { "Unable to save the CPD tokens of file $inputFile for the next analysis." }
            }
            true
        } ?: false
    }

    private fun canSkipUnchangedFiles(sensorContext: SensorContext): Boolean {
        return sensorContext.config().getBoolean(KotlinPlugin.SKIP_UNCHANGED_FILES_OVERRIDE).getOrElse {
            try {
                sensorContext.canSkipUnchangedFiles()
            } catch (_: IncompatibleClassChangeError) {
                false
            }
        }
    }

    private fun fileHasChanged(inputFile: InputFile, contentHashCache: ContentHashCache?): Boolean {
        return contentHashCache?.hasDifferentContentCached(inputFile) ?: (inputFile.status() != InputFile.Status.SAME)
    }
}
