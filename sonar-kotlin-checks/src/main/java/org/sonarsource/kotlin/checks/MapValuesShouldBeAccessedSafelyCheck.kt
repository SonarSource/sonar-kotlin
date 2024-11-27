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

import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeNullability
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.getNextSiblingIgnoringWhitespace
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.reporting.message
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.analyze

@Rule(key = "S6611")
class MapValuesShouldBeAccessedSafelyCheck : CallAbstractCheck() {

    private val issueMessage = message {
        code("Map")
        +" values should be accessed safely. Using the non-null assertion operator here can throw a NullPointerException."
    }

    override val functionsToVisit = listOf(
            FunMatcher(name = "get") {
                withDefiningSupertypes("kotlin.collections.Map", "kotlin.collections.MutableMap")
            }
    )

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: KaFunctionCall<*>, kotlinFileContext: KotlinFileContext) {
        val sibling = callExpression.getParentOfType<KtDotQualifiedExpression>(true)?.getNextSiblingIgnoringWhitespace()
        if (sibling is KtOperationReferenceExpression && sibling.operationSignTokenType == KtTokens.EXCLEXCL) {
            kotlinFileContext.reportIssue(callExpression.parent.parent, issueMessage)
        }
    }

    override fun visitClass(klass: KtClass, context: KotlinFileContext) {
        val arrayAccessExpressions = klass.collectDescendantsOfType<KtArrayAccessExpression> {
            checkSuperType(it)
        }

        arrayAccessExpressions.forEach {
            val sibling = it.getNextSiblingIgnoringWhitespace()
            if (sibling is KtOperationReferenceExpression && sibling.operationSignTokenType == KtTokens.EXCLEXCL)
                context.reportIssue(it.parent, issueMessage)
        }
    }

    private fun checkSuperType(arrayAccessExpression: KtArrayAccessExpression): Boolean = analyze {
        val type = arrayAccessExpression.arrayExpression?.expressionType ?: return false
        if (checkIfSubtype(type)) return true
        return type.allSupertypes.any {
            checkIfSubtype(it.withNullability(KaTypeNullability.NON_NULLABLE))
        }
    }

    private fun checkIfSubtype(type: KaType) = type.symbol?.classId?.asFqNameString() == "kotlin.collections.Map"
            || type.symbol?.classId?.asFqNameString() == "kotlin.collections.MutableMap"

}
