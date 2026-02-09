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
package org.sonarsource.kotlin.api.visiting

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
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

@OptIn(KaImplementationDetail::class)
internal class SonarKaSession(
    private val originalKaSession: KaSession
) : KaSession by originalKaSession {
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
