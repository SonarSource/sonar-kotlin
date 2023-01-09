/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2023 SonarSource SA
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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtPsiUtil.deparenthesize
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getQualifiedExpressionForSelector
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.plugin.KotlinFileContext

private val JAVA_CLASS_KEYWORDS = listOf("java", "javaClass")

@Rule(key = "S6202")
class IsInstanceMethodCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(
        FunMatcher(qualifier = "kotlin.reflect.KClass", name = "isInstance") { withArguments("kotlin.Any") },
        FunMatcher(qualifier = "java.lang.Class", name = "isInstance") { withArguments("kotlin.Any") },
    )

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: ResolvedCall<*>, kotlinFileContext: KotlinFileContext) {
        callExpression.getQualifiedExpressionForSelector()?.receiverExpression
            .qualifiedName(kotlinFileContext, true)?.let { className ->
                kotlinFileContext.reportIssue(callExpression.calleeExpression!!, "Replace this usage of \"isInstance\" with \"is $className\".")
            }
    }

    private fun PsiElement?.qualifiedName(ctx: KotlinFileContext, onlyClass: Boolean): String? {
        return when (val expr = deparenthesize(this as? KtExpression)) {
            is KtClassLiteralExpression -> if (onlyClass) expr.lhs.qualifiedName(ctx, true) else null
            is KtNameReferenceExpression -> if (!onlyClass || expr.isClass(ctx)) expr.getReferencedName() else null
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

    private fun KtReferenceExpression.isClass(ctx: KotlinFileContext) =
        ctx.bindingContext.get(BindingContext.REFERENCE_TARGET, this) is ClassDescriptor

}
