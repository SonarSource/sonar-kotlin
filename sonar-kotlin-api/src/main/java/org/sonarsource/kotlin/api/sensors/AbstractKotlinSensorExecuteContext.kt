/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonarsource.kotlin.api.sensors

import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.slf4j.Logger
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.sensor.SensorContext
import org.sonarsource.analyzer.commons.ProgressReport
import org.sonarsource.kotlin.api.checks.InputFileContext
import org.sonarsource.kotlin.api.checks.InputFileContextImpl
import org.sonarsource.kotlin.api.common.DEFAULT_KOTLIN_LANGUAGE_VERSION
import org.sonarsource.kotlin.api.common.FAIL_FAST_PROPERTY_NAME
import org.sonarsource.kotlin.api.common.KOTLIN_LANGUAGE_VERSION
import org.sonarsource.kotlin.api.common.KOTLIN_REPOSITORY_KEY
import org.sonarsource.kotlin.api.common.SONAR_ANDROID_DETECTED
import org.sonarsource.kotlin.api.common.SONAR_JAVA_BINARIES
import org.sonarsource.kotlin.api.common.SONAR_JAVA_LIBRARIES
import org.sonarsource.kotlin.api.common.measureDuration
import org.sonarsource.kotlin.api.frontend.Environment
import org.sonarsource.kotlin.api.frontend.KotlinSyntaxStructure
import org.sonarsource.kotlin.api.frontend.KotlinTree
import org.sonarsource.kotlin.api.frontend.ParseException
import org.sonarsource.kotlin.api.frontend.RegexCache
import org.sonarsource.kotlin.api.logging.debug
import org.sonarsource.kotlin.api.visiting.KotlinFileVisitor

private val EMPTY_FILE_CONTENT_PATTERN = Regex("""\s*+""")

abstract class AbstractKotlinSensorExecuteContext(
    private val sensorContext: SensorContext,
    private val inputFiles: Iterable<InputFile>,
    private val progressReport: ProgressReport,
    private val visitors: List<KotlinFileVisitor>,
    private val filenames: List<String>,
    private val logger: Logger
) {
    private val isInAndroidContext: Boolean by lazy {
        sensorContext.config().getBoolean(SONAR_ANDROID_DETECTED).orElse(false)
    }

    val environment: Environment by lazy {
        environment(sensorContext, logger)
    }

    val kotlinFiles: List<KotlinSyntaxStructure> by lazy {
        inputFiles.mapNotNull {
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
    }

    abstract val bindingContext: BindingContext

    fun analyzeFiles(): Boolean {
        try {
            val regexCache = RegexCache()
            progressReport.start(filenames)
            kotlinFiles.filter {
                !EMPTY_FILE_CONTENT_PATTERN.matches(it.inputFile.contents())
            }.forEach { (ktFile, doc, inputFile) ->
                if (sensorContext.isCancelled) return false
                val inputFileContext = InputFileContextImpl(sensorContext, inputFile, isInAndroidContext)
                val tree = KotlinTree(ktFile, doc, bindingContext, getFileDiagnostics(ktFile), regexCache)

                measureDuration(inputFile.filename()) {
                    analyzeFile(inputFileContext, tree)
                }
                progressReport.nextFile()
            }
            return true
        } finally {
            Disposer.dispose(environment.disposable)
        }
    }

    private fun analyzeFile(
        inputFileContext: InputFileContext,
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
                logger.error("Cannot analyse '${inputFileContext.inputFile}' with '$visitorId': ${e.message}", e)
                if (sensorContext.config().getBoolean(FAIL_FAST_PROPERTY_NAME).orElse(false)) {
                    throw IllegalStateException(
                        "Exception in '$visitorId' while analyzing '${inputFileContext.inputFile}'",
                        e
                    )
                }
            }
        }
    }

    private fun getFileDiagnostics(ktFile: KtFile): List<Diagnostic> = diagnostics[ktFile] ?: emptyList()

    private val diagnostics: Map<PsiFile, List<Diagnostic>> by lazy {
        measureDuration("Diagnostics") {
            bindingContext.diagnostics.noSuppression().groupBy { it.psiFile }.toMap()
        }
    }

    private fun logParsingError(inputFile: InputFile, e: ParseException) {
        val position = e.position
        var positionMessage = ""
        if (position != null) {
            positionMessage = "Parse error at position ${position.line()}:${position.lineOffset()}"
        }
        logger.error("Unable to parse file: ${inputFile.uri()}. $positionMessage")
        e.message?.let { logger.error(it) }
    }
}


fun environment(sensorContext: SensorContext, logger: Logger): Environment = Environment(
    sensorContext.config().getStringArray(SONAR_JAVA_BINARIES).toList() +
        sensorContext.config().getStringArray(SONAR_JAVA_LIBRARIES).toList(),
    determineKotlinLanguageVersion(sensorContext, logger),
)

private fun determineKotlinLanguageVersion(sensorContext: SensorContext, logger: Logger) =
    (sensorContext.config()[KOTLIN_LANGUAGE_VERSION].map { versionString ->
        LanguageVersion.fromVersionString(versionString).also { langVersion ->
            if (langVersion == null && versionString.isNotBlank()) {
                logger.warn("Failed to find Kotlin version '$versionString'. Defaulting to ${DEFAULT_KOTLIN_LANGUAGE_VERSION.versionString}")
            }
        }
    }.orElse(null) ?: DEFAULT_KOTLIN_LANGUAGE_VERSION)
        .also { logger.debug { "Using Kotlin ${it.versionString} to parse source code" } }

private fun toParseException(action: String, inputFile: InputFile, cause: Throwable) =
    ParseException("Cannot $action '$inputFile': ${cause.message}", (cause as? ParseException)?.position, cause)
