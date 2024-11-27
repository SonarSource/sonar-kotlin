/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonarsource.kotlin.checks

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtEnumEntrySuperclassReferenceExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@org.sonarsource.kotlin.api.frontend.K1only("rewritten, but K2 differs")
@Rule(key = "S1874")
class DeprecatedCodeUsedCheck : AbstractCheck() {

    override fun visitKtFile(file: KtFile, context: KotlinFileContext) = analyze(file) {
        // https://kotlin.github.io/analysis-api/diagnostics.html#diagnostics-in-a-ktfile
        // TODO this doesn't work anymore for K1 because we clear bindingContext.diagnostics
        file.collectDiagnostics(KaDiagnosticCheckerFilter.ONLY_COMMON_CHECKERS)
            .filter { it.factoryName == Errors.DEPRECATION.name }
            .forEach { context.reportIssue(it.psi.elementToReport(), "Deprecated code should not be used.") }

        context.diagnostics
            .filter { it.factory == Errors.DEPRECATION }
            .forEach { context.reportIssue(it.psiElement.elementToReport(), "Deprecated code should not be used.") }
    }

}

private fun PsiElement.elementToReport() = when (this) {
    is KtCallExpression -> calleeExpression
    is KtEnumEntrySuperclassReferenceExpression -> getParentOfType<KtSuperTypeCallEntry>(false)
        ?.valueArgumentList
        ?.leftParenthesis
    else -> this
} ?: this
