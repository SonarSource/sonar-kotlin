/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.kotlin.api.visiting

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.KaCompilationResult
import org.jetbrains.kotlin.analysis.api.components.KaCompilerTarget
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnostic
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeReference
import org.sonarsource.kotlin.api.checks.InputFileContext
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.frontend.KotlinTree

/**
 * Executes the given [action] in a [KaSession] context
 * providing access to [Kotlin Analysis API](https://kotl.in/analysis-api).
 */
inline fun <R> withKaSession(action: KaSession.() -> R): R = action(kaSession!!)

@PublishedApi
internal var kaSession: KaSession? = null

internal class SonarKaSession(
    private val originalKaSession: KaSession
) : KaSession by originalKaSession {
    /**
     * Unlike original
     * [org.jetbrains.kotlin.analysis.api.components.KaTypeProvider.type]
     * instead of
     * [raising exception](https://github.com/JetBrains/kotlin/blob/v2.1.10/analysis/analysis-api/src/org/jetbrains/kotlin/analysis/api/components/KaTypeProvider.kt#L81-L86)
     * such as
     *
     * > org.jetbrains.kotlin.analysis.low.level.api.fir.api.InvalidFirElementTypeException: For TYPE_REFERENCE with text `Any`, the element of type interface org.jetbrains.kotlin.fir.FirElement expected, but no element found
     *
     * returns [org.jetbrains.kotlin.analysis.api.types.KaErrorType] in case of absence of some types.
     */
    override val KtTypeReference.type: KaType
        get() {
            return try {
                with(originalKaSession) {
                    this@type.type
                }
            } catch (e: Exception) {
                // TODO log?
                buildClassType(ClassId.fromString("<error>"))
            }
        }

    /**
     * Always throws [UnsupportedOperationException] to get rid of dependency on codegen.
     */
    @OptIn(KaExperimentalApi::class)
    override fun compile(
        file: KtFile,
        configuration: CompilerConfiguration,
        target: KaCompilerTarget,
        allowedErrorFilter: (KaDiagnostic) -> Boolean,
    ): KaCompilationResult =
        throw UnsupportedOperationException()
}

/**
 * Manages lifetime of [kaSession].
 */
internal inline fun kaSession(ktFile: KtFile, action: () -> Unit) {
    check(kaSession == null)
    try {
        analyze(ktFile) {
            kaSession = SonarKaSession(this)
            action()
        }
    } finally {
        kaSession = null
    }
}

abstract class KotlinFileVisitor {
    fun scan(fileContext: InputFileContext, root: KotlinTree) {
        kaSession(root.psiFile) {
            visit(KotlinFileContext(fileContext, root.psiFile, kaSession!!, root.regexCache))
        }
    }

    abstract fun visit(kotlinFileContext: KotlinFileContext)
}
