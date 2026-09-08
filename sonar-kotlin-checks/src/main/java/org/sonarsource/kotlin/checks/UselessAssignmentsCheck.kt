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
package org.sonarsource.kotlin.checks

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.diagnostics.KaSeverity
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

@Rule(key = "S6615")
class UselessAssignmentsCheck : AbstractCheck() {

    override fun visitKtFile(file: KtFile, context: KotlinFileContext) {
        val diagnostics = context.kaDiagnostics.toList()
        val declarationsWithErrors = diagnostics
            .filter { it.severity == KaSeverity.ERROR }
            .mapNotNullTo(mutableSetOf()) { containingDeclaration(it.psi) }

        diagnostics
            .mapNotNull { diagnostic ->
                when (diagnostic.factoryName) {
                    FirErrors.VARIABLE_INITIALIZER_IS_REDUNDANT.name ->
                        diagnostic.psi to "Remove this useless initializer."

                    FirErrors.VARIABLE_NEVER_READ.name ->
                        (diagnostic.psi as KtNamedDeclaration).identifyingElement!! to
                        "Remove this variable, which is assigned but never accessed."

                    FirErrors.ASSIGNED_VALUE_IS_NEVER_READ.name ->
                        if (containingDeclaration(diagnostic.psi) in declarationsWithErrors) null
                        else withKaSession { diagnostic.psi.parent to "The value assigned here is never used." }

                    else -> null
                }
            }.forEach { (element, msg) -> context.reportIssue(element, msg) }
    }

    private fun containingDeclaration(element: PsiElement): KtDeclarationWithBody? =
        containingDeclarations(element).firstOrNull()

    private fun containingDeclarations(element: PsiElement): Sequence<KtDeclarationWithBody> =
        element.parentsWithSelf
            .filterIsInstance<KtDeclarationWithBody>()
            // Lambda and anonymous-function diagnostics belong to their enclosing named declaration.
            .filter { declaration ->
                declaration !is KtFunctionLiteral &&
                    (declaration !is KtNamedFunction || declaration.name != null)
            }
}
