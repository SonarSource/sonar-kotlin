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

import org.jetbrains.kotlin.codegen.kotlinType
import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespace
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.plugin.KotlinFileContext

@Rule(key = "S6611")
class MapValuesShouldBeAccessedSafelyCheck : CallAbstractCheck() {

    override val functionsToVisit = listOf(
            FunMatcher {
                withQualifiers("kotlin.collections.Map", "kotlin.collections.MutableMap")
                withNames("get", "getValue", "getOrElse", "getOrDefault", "null")
            }
    )

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: ResolvedCall<*>, kotlinFileContext: KotlinFileContext) {
        val sibling = callExpression.getParentOfType<KtDotQualifiedExpression>(true)?.getNextSiblingIgnoringWhitespace()
        if (sibling is KtOperationReferenceExpression && sibling.operationSignTokenType == KtTokens.EXCLEXCL) {
            kotlinFileContext.reportIssue(callExpression.parent.parent, "\"Map\" values should be accessed safely. Using the non-null assertion operator here can throw an unexpected NullPointerException.")
        }
    }

    override fun visitClass(klass: KtClass, context: KotlinFileContext) {
        val arrayAccessExpressions = klass.collectDescendantsOfType<KtArrayAccessExpression> {
            it.arrayExpression.kotlinType(context.bindingContext)?.getKotlinTypeFqName(false) == "kotlin.collections.Map"
                    || it.arrayExpression.kotlinType(context.bindingContext)?.getKotlinTypeFqName(false) == "kotlin.collections.MutableMap"
        }

        arrayAccessExpressions.forEach {
            val sibling = it.getNextSiblingIgnoringWhitespace()
            if (sibling is KtOperationReferenceExpression && sibling.operationSignTokenType == KtTokens.EXCLEXCL)
                context.reportIssue(it.parent, "\"Map\" values should be accessed safely. Using the non-null assertion operator here can throw an unexpected NullPointerException.")
        }
    }

}
