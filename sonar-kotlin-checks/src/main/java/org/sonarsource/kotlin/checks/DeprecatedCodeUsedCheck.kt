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
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstructorCalleeExpression
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtEnumEntrySuperclassReferenceExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.KtSuperTypeEntry
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parents
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

@Rule(key = "S1874")
class DeprecatedCodeUsedCheck : AbstractCheck() {

    override fun visitKtFile(file: KtFile, context: KotlinFileContext) = withKaSession {
        context.kaDiagnostics
            .filter { it.factoryName == FirErrors.DEPRECATION.name }
            .filterNot { it.psi.isInsideDeprecatedScope() }
            .filterNot { it.psi.isTypeReferencePosition() }
            .distinctBy { it.psi }
            .forEach { context.reportIssue(it.psi.elementToReport(), "Deprecated code should not be used.") }
    }

}

private fun PsiElement.isTypeReferencePosition(): Boolean {
    val typeRef = (sequenceOf(this) + parents).filterIsInstance<KtTypeReference>().firstOrNull() ?: return false
    return when (typeRef.parent) {
        // actual usages of the deprecated element — must be reported
        is KtConstructorCalleeExpression,   // class Foo : DeprecatedClass() / @DeprecatedAnnotation[()]
        is KtSuperTypeEntry,                // class Foo : DeprecatedInterface
        is KtDelegatedSuperTypeEntry,       // class Foo : DeprecatedInterface by d
        is KtIsExpression,                  // x is DeprecatedCode
        is KtBinaryExpressionWithTypeRHS -> // x as DeprecatedCode
            false
        // structural / signature positions — suppress
        else -> true
    }
}

private fun PsiElement.isInsideDeprecatedScope(): Boolean =
    parents.filterIsInstance<KtAnnotated>()
        .any { annotated -> annotated.annotationEntries.any { it.shortName?.asString() == "Deprecated" } }

private fun PsiElement.elementToReport() = when (this) {
    is KtCallExpression -> calleeExpression
    is KtEnumEntrySuperclassReferenceExpression -> getParentOfType<KtSuperTypeCallEntry>(false)
        ?.valueArgumentList
        ?.leftParenthesis
    else -> this
} ?: this
