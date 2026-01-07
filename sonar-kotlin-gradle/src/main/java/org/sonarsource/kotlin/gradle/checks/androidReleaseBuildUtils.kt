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
import org.jetbrains.kotlin.psi.KtValueArgument
import org.sonarsource.kotlin.api.checks.predictRuntimeStringValue

private const val settingsGradleFileName = "settings.gradle.kts"

internal fun KtFile.isSettingGradleKts() = name.endsWith(settingsGradleFileName, ignoreCase = true)

internal fun KtFunctionLiteral.getApplicationId() =
        getChildCallWithLambdaOrNull("defaultConfig")
        ?.lambda
        ?.getPropertyAssignmentOrNull("applicationId")

internal fun KtElement.getChildCallOrNull(childCallName: String): KtCallExpression? =
    descendantsOfType<KtCallExpression>().firstOrNull { it.calleeExpression?.text == childCallName }

internal fun KtElement.getChildCallWithLambdaOrNull(childCallName: String): CalleeAndLambda? {
    val callee = getChildCallOrNull(childCallName) ?: return null
    val lambda = callee.functionLiteralArgumentOrNull() ?: return null
    return CalleeAndLambda(callee.calleeExpression!!, lambda)
}

internal fun KtElement.getGetByNameCallWithLambdaOrNull(): CalleeAndLambda? {
    val callee = descendantsOfType<KtCallExpression>().firstOrNull {
        it.calleeExpression?.text == "getByName" &&
            it.valueArguments.size == 2 &&
            it.valueArguments[0].isReleaseBuildType()
    } ?: return null
    val lambda = callee.functionLiteralArgumentOrNull() ?: return null
    return CalleeAndLambda(callee.calleeExpression!!, lambda)
}

internal fun KtValueArgument.isReleaseBuildType(): Boolean =
    textMatches("BuildType.RELEASE") ||
        getArgumentExpression()?.predictRuntimeStringValue() == "release"

internal fun KtCallExpression.functionLiteralArgumentOrNull(): KtFunctionLiteral? =
    valueArguments
        .flatMap { it.childrenOfType<KtLambdaExpression>() }
        .flatMap { it.childrenOfType<KtFunctionLiteral>() }
        .singleOrNull()

internal fun KtElement.getPropertyAssignmentOrNull(propertyName: String): KtBinaryExpression? =
    descendantsOfType<KtBinaryExpression>().firstOrNull { it.operationToken == KtTokens.EQ && it.left?.text == propertyName }

internal data class CalleeAndLambda(val callee: KtExpression, val lambda: KtFunctionLiteral)

