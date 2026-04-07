/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1.0.1, as published by SonarSource Sàrl.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.kotlin.api.sensors

import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.impl.ZipHandler
import java.io.File
import java.io.IOException
import kotlin.jvm.optionals.getOrElse
import org.jetbrains.kotlin.config.LanguageVersion
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
import org.sonarsource.kotlin.api.common.measureDuration
import org.sonarsource.kotlin.api.frontend.Environment
import org.sonarsource.kotlin.api.frontend.KotlinFileSystem
import org.sonarsource.kotlin.api.frontend.KotlinSyntaxStructure
import org.sonarsource.kotlin.api.frontend.KotlinTree
import org.sonarsource.kotlin.api.frontend.KotlinVirtualFile
import org.sonarsource.kotlin.api.frontend.ParseException
import org.sonarsource.kotlin.api.frontend.createK2AnalysisSession
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

    private val inputFileToVirtualFile: Map<InputFile, KotlinVirtualFile> by lazy {
        val virtualFileSystem = KotlinFileSystem()

        inputFiles.associateWith {
            KotlinVirtualFile(
                virtualFileSystem,
                File(it.uri().rawPath),
                contentProvider = {
                    try {
                        it.contents()
                    } catch (_: IOException) {
                        ""
                    }
                },
            )
        }
    }

    abstract val classpath: List<String>

    val environment: Environment by lazy {
        /** [analyzeFiles] */
        val env = Environment(
            Disposer.newDisposable(),
            classpath,
            determineKotlinLanguageVersion(sensorContext, logger),
        )

        env.k2session = createK2AnalysisSession(
            env.disposable,
            env.configuration,
            inputFileToVirtualFile.values,
        )
        return@lazy env
    }

    val kotlinFiles: List<KotlinSyntaxStructure> by lazy {
        inputFiles.mapNotNull {
            val inputFileContext = InputFileContextImpl(sensorContext, it, isInAndroidContext)
            try {
                // The current logic relies on eager loading of all files before the analysis starts.
                // To trigger potential IO exceptions and report them, we need to do this call.
                // TODO SONARKT-711
                it.contents()
                KotlinSyntaxStructure.of(environment, it, inputFileToVirtualFile.getValue(it))
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

    fun analyzeFiles(): Boolean {
        try {
            progressReport.start(filenames)
            kotlinFiles.filter {
                !EMPTY_FILE_CONTENT_PATTERN.matches(it.inputFile.contents())
            }.forEach { (ktFile, doc, inputFile) ->
                if (sensorContext.isCancelled) return false
                val inputFileContext = InputFileContextImpl(sensorContext, inputFile, isInAndroidContext)
                val tree = KotlinTree(ktFile, doc)

                measureDuration(inputFile.filename()) {
                    analyzeFile(inputFileContext, tree)
                }
                progressReport.nextFile()
            }
            return true
        } finally {
            Disposer.dispose(environment.disposable)
            // When FastJarFileSystem is unavailable (e.g. when sun.misc.Unsafe is inaccessible due to
            // missing --add-opens flags, a security manager, or the JDK variant in use), the Kotlin
            // compiler falls back to ZipHandler (ZipFile-based). ZipHandler holds static ZipFile handles
            // in a static cache that are never released by the disposal chain, causing file locks on
            // Windows that prevent Maven from overwriting dependencies. Explicitly clear the cache here.
            // Note: clearFileAccessorCache() clears the global static cache (shared across all analyses),
            // but this is safe because analyses run serially (SonarQube sensor framework and SonarLint's
            // AnalysisScheduler both serialize analysis execution). Clearing is also safe when
            // FastJarFileSystem IS available, as it uses a separate cache unaffected by this call.
            // If concurrent analysis were ever introduced, this call would need to be revisited
            // as it clears handles that may still be in use by a parallel analysis.
            ZipHandler.clearFileAccessorCache()
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
                if (sensorContext.config().getBoolean(FAIL_FAST_PROPERTY_NAME).getOrElse { false }) {
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
        logger.error("Unable to parse file: ${inputFile.uri()}. $positionMessage")
        e.message?.let { logger.error(it) }
        logger.debug("Detailed information: ", e)
    }
}

internal fun determineKotlinLanguageVersion(sensorContext: SensorContext, logger: Logger) =
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
