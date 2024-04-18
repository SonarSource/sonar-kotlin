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
package org.sonarsource.kotlin.api.frontend

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.sonarsource.kotlin.api.checks.InputFileContext
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.reporting.SecondaryLocation

data class KotlinFileContext(
    val inputFileContext: InputFileContext,
    val ktFile: KtFile,
    val bindingContext: BindingContext,
    val diagnostics: List<Diagnostic>,
    val regexCache: RegexCache,
)

fun KotlinFileContext.secondaryOf(psiElement: PsiElement, msg: String? = null) = SecondaryLocation(textRange(psiElement), msg)


private fun KotlinFileContext.doNothing() {
    //val sessionProvider = KtFirAnalysisSessionProvider(ktFile.project as com.intellij.openapi.project.Project)
    // analyse function to which we give a lambda
    org.jetbrains.kotlin.analysis.api.analyze(ktFile) {
        // todo try new features and rewrite a rule
    }

}
