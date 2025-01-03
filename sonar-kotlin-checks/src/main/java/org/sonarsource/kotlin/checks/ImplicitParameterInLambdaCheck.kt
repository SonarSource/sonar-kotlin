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
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtValueArgument
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.reporting.message
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S6558")
class ImplicitParameterInLambdaCheck : AbstractCheck() {

    override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression, kotlinFileContext: KotlinFileContext) {
        if (lambdaExpression.isArgumentWithImplicitParameterDeclarationWithoutReferenceType()
            || lambdaExpression.isPropertyWithReferenceTypeAndImplicitParameterDeclaration()) {

            // At this point we process lambda that have a single parameter
            val lambdaValueParameterList = lambdaExpression.functionLiteral.valueParameterList!!
            kotlinFileContext.reportIssue(psiElement = lambdaValueParameterList, message = buildReportMessage())
        }
    }

    private fun KtLambdaExpression.isArgumentWithImplicitParameterDeclarationWithoutReferenceType() =
        isArgument() && hasImplicitParameter() && valueParameters[0].typeReference == null

    private fun KtLambdaExpression.isArgument() =
        parent.skipParentParentheses().let { it is KtLambdaArgument || it is KtValueArgument }

    private fun KtLambdaExpression.isPropertyWithReferenceTypeAndImplicitParameterDeclaration() =
        isPropertyWithTypeReference() && hasImplicitParameter()

    private fun KtLambdaExpression.isPropertyWithTypeReference() =
        (parent.skipParentParentheses() as? KtProperty)?.typeReference != null
}

private fun KtLambdaExpression.hasImplicitParameter() = valueParameters.size == 1 && valueParameters[0].name == "it"

private fun buildReportMessage() =
    message {
        +"Remove this "
        code("it")
        +" parameter declaration or give this lambda parameter a meaningful name."
    }
