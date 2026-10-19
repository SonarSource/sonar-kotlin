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
package org.sonarsource.kotlin.api.visiting

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.KaCompilationOptions
import org.jetbrains.kotlin.analysis.api.components.KaCompilationResult
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.analysis.api.resolution.KaCallInfo
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.idea.references.KtReference
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtDeclarationWithReturnType
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.utils.exceptions.KotlinIllegalArgumentExceptionWithAttachments
import org.slf4j.LoggerFactory
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
     * Unlike originals, the overrides below do not propagate exception such as
     *
     * > org.jetbrains.kotlin.utils.exceptions.KotlinIllegalArgumentExceptionWithAttachments: Error while resolving org.jetbrains.kotlin.fir.declarations.impl.FirNamedFunctionImpl
     * >
     * > Caused by: java.lang.IllegalStateException: Incorrect type: org/jetbrains/kotlin/descriptors/Substitutable, subType: it(org/jetbrains/kotlin/descriptors/Substitutable<*> & org/jetbrains/kotlin/descriptors/VariableDescriptor), superType: org/jetbrains/kotlin/descriptors/Substitutable<*>
     *
     * which is raised since Kotlin 2.4.21 when resolving body of a function which contains smart cast to intersection type,
     * and instead return the same result as in case of absence of semantic information.
     */
    private inline fun <R> recoverFromResolveError(fallback: () -> R, action: KaSession.() -> R): R =
        try {
            originalKaSession.action()
        } catch (e: KotlinIllegalArgumentExceptionWithAttachments) {
            LOG.debug("Unable to resolve", e)
            fallback()
        }

    /**
     * Returns `null` on resolve error, as if [this] did not correspond to a call.
     */
    override fun KtElement.resolveToCall(): KaCallInfo? =
        recoverFromResolveError({ null }) { this@resolveToCall.resolveToCall() }

    /**
     * Returns `null` on resolve error, as if [this] were unresolved.
     */
    override fun KtReference.resolveToSymbol(): KaSymbol? =
        recoverFromResolveError({ null }) { this@resolveToSymbol.resolveToSymbol() }

    /**
     * Returns `null` on resolve error, as if [this] were not a value.
     */
    override val KtExpression.expressionType: KaType?
        get() = recoverFromResolveError({ null }) { this@expressionType.expressionType }

    /**
     * Returns [org.jetbrains.kotlin.analysis.api.types.KaErrorType] on resolve error.
     */
    override val KtTypeReference.type: KaType
        get() = recoverFromResolveError({ buildClassType(ClassId.fromString("<error>")) }) { this@type.type }

    /**
     * Returns [org.jetbrains.kotlin.analysis.api.types.KaErrorType] on resolve error.
     */
    override val KtDeclarationWithReturnType.returnType: KaType
        get() = recoverFromResolveError({ buildClassType(ClassId.fromString("<error>")) }) { this@returnType.returnType }

    /**
     * Returns no diagnostics on resolve error.
     */
    override fun KtFile.collectDiagnostics(filter: KaDiagnosticCheckerFilter): Collection<KaDiagnosticWithPsi<*>> =
        recoverFromResolveError({ emptyList() }) { this@collectDiagnostics.collectDiagnostics(filter) }

    /**
     * Always throws [UnsupportedOperationException] to get rid of dependency on codegen.
     */
    @OptIn(KaExperimentalApi::class)
    override fun compile(
        file: KtFile,
        options: KaCompilationOptions,
    ): KaCompilationResult =
        throw UnsupportedOperationException()

    companion object {
        private val LOG = LoggerFactory.getLogger(SonarKaSession::class.java)
    }
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
