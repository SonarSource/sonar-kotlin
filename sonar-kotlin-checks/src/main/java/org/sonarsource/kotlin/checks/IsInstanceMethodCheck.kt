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
package org.sonarsource.kotlin.checks

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

private val JAVA_CLASS_KEYWORDS = listOf("java", "javaClass")

@Rule(key = "S6202")
class IsInstanceMethodCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(
        FunMatcher(qualifier = "kotlin.reflect.KClass", name = "isInstance") { withArguments("kotlin.Any") },
        FunMatcher(qualifier = "java.lang.Class", name = "isInstance") { withArguments("kotlin.Any") },
    )

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: KaFunctionCall<*>, kotlinFileContext: KotlinFileContext) {
        callExpression.getQualifiedExpressionForSelector()?.receiverExpression
            .qualifiedName(kotlinFileContext, true)?.let { className ->
                kotlinFileContext.reportIssue(callExpression.calleeExpression!!, "Replace this usage of \"isInstance\" with \"is $className\".")
            }
    }

    private fun PsiElement?.qualifiedName(ctx: KotlinFileContext, onlyClass: Boolean): String? {
        return when (val expr = deparenthesize(this as? KtExpression)) {
            is KtClassLiteralExpression -> if (onlyClass) expr.lhs.qualifiedName(ctx, true) else null
            is KtNameReferenceExpression -> if (!onlyClass || expr.isClass()) expr.getReferencedName() else null
            is KtDotQualifiedExpression -> {
                val right = expr.selectorExpression.qualifiedName(ctx, onlyClass)
                if (right != null) {
                    expr.receiverExpression.qualifiedName(ctx, false)?.let { left -> "$left.$right" }
                } else {
                    expr.qualifiedNameWithoutJavaClassKeyword(ctx, onlyClass)
                }
            }
            else -> null
        }
    }

    private fun KtDotQualifiedExpression.qualifiedNameWithoutJavaClassKeyword(ctx: KotlinFileContext, onlyClass: Boolean): String? =
        if (onlyClass && isJavaClassKeyword(selectorExpression)) receiverExpression.qualifiedName(ctx, true) else null

    private fun isJavaClassKeyword(expr: KtExpression?): Boolean =
        (expr is KtNameReferenceExpression) && (expr.getReferencedName() in JAVA_CLASS_KEYWORDS)

    private fun KtReferenceExpression.isClass(): Boolean = withKaSession {
        return this@isClass.mainReference.resolveToSymbol() is KaClassSymbol
    }

}
