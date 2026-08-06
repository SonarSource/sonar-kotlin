/*
 * SonarSource Kotlin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.sonar.api.notifications.AnalysisWarnings
import org.sonarsource.api.sonarlint.SonarLintSide
import org.sonarsource.analyzer.commons.ProgressReport
import org.sonarsource.analyzer.commons.appsec.TestFileClassifier
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
    private val testFiles: TestFileClassifier by lazy {
        TestFileClassifier.of(
            sensorContext.config(),
            "**/test/**", "**/tests/**",
            "**/*Test.kt", "**/*Tests.kt", "**/*Spec.kt", "**/*IT.kt")
    }

    private fun isTestFile(inputFile: InputFile): Boolean =
        inputFile.type() == InputFile.Type.TEST || testFiles.looksLikeTestFile(inputFile)

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

    open fun onFileRead() {
        // no-op by default; subclasses override to react to each file being processed (e.g. increment a telemetry counter)
    }

    open fun onParseFailure() {
        // no-op by default; subclasses override to react to parse failures (e.g. increment a telemetry counter)
    }

    open fun onReadFailure() {
        // no-op by default; subclasses override to react to read failures (e.g. increment a telemetry counter)
    }

    open fun onAnalysisCrash(inputFile: InputFile) {
        // no-op by default; subclasses react to a recoverable per-file analysis crash (e.g. a StackOverflowError)
    }

    open fun onAnalysisComplete() {
        // no-op by default; called once after the per-file loop (a normally-completing scan) so subclasses can
        // post aggregated results such as a customer-visible warning about partially-analyzed files
    }

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
            onFileRead()
            val inputFileContext = InputFileContextImpl(sensorContext, it, isInAndroidContext, isTestFile(it))
            try {
                // The current logic relies on eager loading of all files before the analysis starts.
                // To trigger potential IO exceptions and report them, we need to do this call.
                // TODO SONARKT-711
                it.contents()
                KotlinSyntaxStructure.of(environment, it, inputFileToVirtualFile.getValue(it))
            } catch (e: ParseException) {
                logParsingError(it, toParseException("parse", it, e))
                inputFileContext.reportAnalysisParseError(KOTLIN_REPOSITORY_KEY, it, e.position)
                onParseFailure()
                null
            } catch (e: Exception) {
                val parseException = toParseException("read", it, e)
                logParsingError(it, parseException)
                inputFileContext.reportAnalysisParseError(KOTLIN_REPOSITORY_KEY, it, parseException.position)
                onReadFailure()
                null
            } catch (e: StackOverflowError) {
                // A StackOverflowError is a java.lang.Error, not an Exception, so it escapes the catch above.
                // It can originate in the recursive PSI construction of a deeply-nested file. Contain it here so
                // the up-front parse of all files does not abort the whole scan. Keep the handler stack-frugal:
                // log a short message and do NOT pass the throwable to the logger (avoid formatting its huge trace).
                inputFileContext.reportAnalysisParseError(KOTLIN_REPOSITORY_KEY, it, null)
                onAnalysisCrash(it)
                logger.error("Cannot parse '$it': ${e.javaClass.name}")
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
                val inputFileContext = InputFileContextImpl(sensorContext, inputFile, isInAndroidContext, isTestFile(inputFile))
                val tree = KotlinTree(ktFile, doc)

                measureDuration(inputFile.filename()) {
                    analyzeFile(inputFileContext, tree)
                }
                progressReport.nextFile()
            }
            onAnalysisComplete()
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
            } catch (e: StackOverflowError) {
                // A StackOverflowError is a java.lang.Error, not an Exception, so it escapes the catch below.
                // It can originate deep in the Kotlin compiler's type resolver (e.g. an invalid cyclic typealias)
                // or in the analyzer's own recursive PSI walkers. Contain it here so a single crashing file is
                // reported and skipped instead of aborting the whole scan. This handler runs at a shallow frame
                // (the deep frames have already unwound), so it has ample stack headroom -- but keep it frugal:
                // log a short message and do NOT pass the throwable to the logger (avoid formatting its huge trace).
                val message = "Analysis of '${inputFileContext.inputFile}' with '$visitorId' failed: ${e.javaClass.name}"
                inputFileContext.reportAnalysisError(message, null)
                logger.error(message)
                onAnalysisCrash(inputFileContext.inputFile)
                if (sensorContext.config().getBoolean(FAIL_FAST_PROPERTY_NAME).getOrElse { false }) {
                    throw IllegalStateException(
                        "Exception in '$visitorId' while analyzing '${inputFileContext.inputFile}'",
                        e
                    )
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

/**
 * Posts a project-level, customer-visible warning (SonarQube background-task warnings) when one or more files
 * crashed during analysis. The crash is recovered (the file is skipped and the scan completes), but recovery is
 * best-effort: the crashing file is not fully analyzed, and because the crash can leave shared analysis state
 * (e.g. the Kotlin compiler's type resolver) in a degraded state, results for *other* files in the same run may
 * also be affected. The warning is therefore phrased about the analysis as a whole rather than only the listed
 * files. No-op when [crashedFiles] is empty. The listed file names are capped so the message cannot balloon on a
 * large poisoned codebase. Unlike telemetry, the scanner log, or a rule-gated issue, [AnalysisWarnings.addUnique]
 * is never gated by profile/rule and always renders in the UI.
 */
fun postAnalysisCrashWarning(analysisWarnings: AnalysisWarnings, crashedFiles: Collection<String>) {
    if (crashedFiles.isEmpty()) return
    val shown = crashedFiles.take(10).joinToString(", ")
    val more = if (crashedFiles.size > 10) " (and ${crashedFiles.size - 10} more)" else ""
    val fileWord = if (crashedFiles.size == 1) "file" else "files"
    analysisWarnings.addUnique(
        "${crashedFiles.size} $fileWord crashed during analysis; the Kotlin analyzer recovered and completed the " +
            "scan, but the analysis may be incomplete: $shown$more."
    )
}

/**
 * A no-op [AnalysisWarnings] made available in the SonarLint analysis container.
 *
 * [AnalysisWarnings] is a `@ScannerSide`-only component: the SonarQube scanner provides it, but SonarLint does not
 * register any implementation, so a sensor that requires it via its (single) constructor cannot be instantiated
 * under SonarLint. Registering this `@SonarLintSide` component in the plugin (only for the SonarLint product) gives
 * the container an [AnalysisWarnings] bean to inject, keeping the sensors single-constructor. It is deliberately
 * *not* `@ScannerSide` so it never competes with the real scanner-provided implementation.
 */
@SonarLintSide
class NoOpAnalysisWarnings : AnalysisWarnings {
    override fun addUnique(text: String) {
        // no-op: SonarLint does not surface analysis warnings
    }
}
