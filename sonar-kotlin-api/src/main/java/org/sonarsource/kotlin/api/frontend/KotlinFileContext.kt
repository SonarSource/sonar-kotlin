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
package org.sonarsource.kotlin.api.frontend

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.diagnostics.KaDiagnosticWithPsi
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.sonarsource.kotlin.api.checks.InputFileContext
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.visiting.withKaSession

data class KotlinFileContext(
    val inputFileContext: InputFileContext,
    val ktFile: KtFile,
    /**
     * @see [org.sonarsource.kotlin.api.visiting.withKaSession]
     */
    @Deprecated("use kotlin-analysis-api instead")
    val bindingContext: Any,
    @Deprecated("use kotlin-analysis-api instead", ReplaceWith("kaDiagnostics"))
    val diagnostics: List<Diagnostic>,
    val regexCache: RegexCache,
) {

    internal var k2Diagnostics: Sequence<KaDiagnosticWithPsi<*>> = emptySequence()

    val kaDiagnostics: Sequence<KaDiagnosticWithPsi<*>> by lazy {
        withKaSession {
//            val k1 = diagnostics.asSequence().map { K1internals.kaFe10Diagnostic(it, token) }
            return@lazy k2Diagnostics
        }
    }

}

fun KotlinFileContext.secondaryOf(psiElement: PsiElement, msg: String? = null) = SecondaryLocation(textRange(psiElement), msg)
