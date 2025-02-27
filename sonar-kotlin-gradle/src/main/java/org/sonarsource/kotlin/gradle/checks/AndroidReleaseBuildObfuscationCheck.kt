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
package org.sonarsource.kotlin.gradle.checks

import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.descendantsOfType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.jetbrains.kotlin.resolve.BindingContext
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.predictRuntimeBooleanValue
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private const val gradleExtension = ".gradle.kts"
private const val message = "Enable obfuscation by setting isMinifiedEnabled."

@Rule(key = "S7204")
class AndroidReleaseBuildObfuscationCheck : AbstractCheck() {

    override fun visitScriptInitializer(initializer: KtScriptInitializer, data: KotlinFileContext) {
        data.ktFile.isGradleKts() || return
        val (_, androidLambda) = initializer.getChildCallWithLambdaOrNull("android") ?: return
        val (_, buildTypesLambda) = androidLambda.getChildCallWithLambdaOrNull("buildTypes") ?: return
        val (releaseCallee, releaseLambda) = buildTypesLambda.getChildCallWithLambdaOrNull("release") ?: return
        if (releaseLambda.getPropertyAssignmentToTrueOrNull("isMinifyEnabled", data.bindingContext) == null) {
            data.reportIssue(releaseCallee, message)
        }
    }

    private fun KtFile.isGradleKts() = name.endsWith(gradleExtension)

    private fun KtElement.getChildCallWithLambdaOrNull(childCallName: String): Pair<KtExpression, KtFunctionLiteral>? {
        val callee = descendantsOfType<KtCallExpression>().firstOrNull { it.calleeExpression?.text == childCallName } ?: return null
        val lambda = callee.functionLiteralArgumentOrNull() ?: return null
        return callee.calleeExpression!! to lambda
    }

    private fun KtCallExpression.functionLiteralArgumentOrNull(): KtFunctionLiteral? =
        valueArguments
            .flatMap { it.childrenOfType<KtLambdaExpression>() }
            .flatMap { it.childrenOfType<KtFunctionLiteral>() }
            .singleOrNull()

    private fun KtElement.getPropertyAssignmentToTrueOrNull(propertyName: String, bindingContext: BindingContext): KtBinaryExpression? =
        descendantsOfType<KtBinaryExpression>().firstOrNull {
            it.operationToken == KtTokens.EQ &&
                it.left?.text == propertyName &&
                it.right?.predictRuntimeBooleanValue(bindingContext) ?: true }
}
