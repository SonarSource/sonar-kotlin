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
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

@Rule(key = "S6615")
class UselessAssignmentsCheck : AbstractCheck() {

    override fun visitKtFile(file: KtFile, context: KotlinFileContext) {
        val diagnostics = context.kaDiagnostics.toList()
        val errorRegions = diagnostics
            .filter { it.severity == KaSeverity.ERROR }
            .mapNotNullTo(mutableSetOf()) { suppressionRegion(it.psi) }

        diagnostics
            .mapNotNull { diagnostic ->
                when (diagnostic.factoryName) {
                    FirErrors.VARIABLE_INITIALIZER_IS_REDUNDANT.name ->
                        diagnostic.psi to "Remove this useless initializer."

                    FirErrors.VARIABLE_NEVER_READ.name ->
                        (diagnostic.psi as KtNamedDeclaration).identifyingElement!! to
                        "Remove this variable, which is assigned but never accessed."

                    FirErrors.ASSIGNED_VALUE_IS_NEVER_READ.name ->
                        if (suppressionRegion(diagnostic.psi) in errorRegions) null
                        else withKaSession { diagnostic.psi.parent to "The value assigned here is never used." }

                    else -> null
                }
            }.forEach { (element, msg) -> context.reportIssue(element, msg) }
    }

    /**
     * The Kotlin compiler may report incorrect assigned-value diagnostics when
     * semantic information is incomplete, for example because symbols are
     * unresolved. We don't have a way to identify these false positives, so
     * this method defines a heuristic reliability boundary. A broader region
     * can suppress valid findings, while a narrower one can leave false
     * positives. Executable declarations, non-local property initialization,
     * delegates, and `init` blocks are treated as independent regions.
     * Lambdas, anonymous functions, and local properties inherit their
     * enclosing region.
     */
    private fun suppressionRegion(element: PsiElement): KtDeclaration? =
        element.parentsWithSelf
            .filterIsInstance<KtDeclaration>()
            .firstOrNull { declaration ->
                when (declaration) {
                    // Lambda and anonymous-function diagnostics belong to their enclosing declaration.
                    is KtFunctionLiteral -> false
                    is KtNamedFunction -> declaration.name != null
                    is KtDeclarationWithBody, is KtAnonymousInitializer -> true
                    // Local variables stay transparent and belong to the enclosing declaration.
                    is KtProperty -> !declaration.isLocal
                    else -> false
                }
            }
}
