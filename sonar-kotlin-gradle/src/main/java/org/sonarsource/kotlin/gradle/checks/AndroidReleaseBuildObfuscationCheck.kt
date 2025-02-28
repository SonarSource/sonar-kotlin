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
import org.jetbrains.kotlin.psi.KtValueArgument
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.reporting.SecondaryLocation

private const val gradleExtension = ".gradle.kts"
private const val mainMessage = "Make sure that obfuscation is enabled in the release build configuration."
private const val debuggableSetToTrueMessage = "Enabling debugging disables obfuscation for this release build. Make sure this is safe here."

private val releaseBuildTypeExpressions = listOf("\"release\"", "\"\"\"release\"\"\"", "BuildType.RELEASE")

@Rule(key = "S7204")
class AndroidReleaseBuildObfuscationCheck : AbstractCheck() {

    override fun visitScriptInitializer(initializer: KtScriptInitializer, data: KotlinFileContext) {
        data.ktFile.isGradleKts() || return
        val (androidCallee, androidLambda) = initializer.getChildCallWithLambdaOrNull("android") ?: return

        // Ensure the project is an Android app, and not a library
        val (_, defaultConfigLambda) = androidLambda.getChildCallWithLambdaOrNull("defaultConfig") ?: return
        defaultConfigLambda.getPropertyAssignmentOrNull("applicationId") ?: return

        val buildTypes = androidLambda.getChildCallWithLambdaOrNull("buildTypes")
        if (buildTypes == null) {
            data.reportIssue(androidCallee, mainMessage)
            return
        }

        val (_, buildTypesLambda) = buildTypes
        val (releaseCallee, releaseLambda) = buildTypesLambda.getChildCallWithLambdaOrNull("release")
            ?: androidLambda.getGetByNameCallWithLambdaOrNull()
            ?: return

        // TODO: use predictRuntimeBooleanValue for "true" and "false" after migration to K2
        val isDebuggableAssignment = releaseLambda.getPropertyAssignmentOrNull("isDebuggable")
        val isDebuggableSetToTrue = isDebuggableAssignment?.right?.text == "true"
        val isMinifiedEnabledAssignment = releaseLambda.getPropertyAssignmentOrNull("isMinifyEnabled")
        val isMinifiedEnabledAssignmentNotSet = isMinifiedEnabledAssignment == null
        val isMinifiedEnabledAssignmentSetToFalse = isMinifiedEnabledAssignment?.right?.text == "false"
        val proguardFilesNotCalled = releaseLambda.getChildCallOrNull("proguardFiles") == null

        when {
            isDebuggableSetToTrue && !isMinifiedEnabledAssignmentNotSet && !isMinifiedEnabledAssignmentSetToFalse ->
                data.reportIssue(
                    releaseCallee,
                    debuggableSetToTrueMessage,
                    secondaryLocations = listOf(isDebuggableAssignment!!).map { SecondaryLocation(data.textRange(it), "") }
                )
            isMinifiedEnabledAssignmentNotSet -> data.reportIssue(releaseCallee, mainMessage)
            isMinifiedEnabledAssignmentSetToFalse -> data.reportIssue(isMinifiedEnabledAssignment!!, mainMessage)
            proguardFilesNotCalled -> data.reportIssue(releaseCallee, mainMessage)
        }
    }

    private fun KtFile.isGradleKts() = name.endsWith(gradleExtension)

    private fun KtElement.getChildCallOrNull(childCallName: String): KtCallExpression? =
        descendantsOfType<KtCallExpression>().firstOrNull { it.calleeExpression?.text == childCallName }

    private fun KtElement.getChildCallWithLambdaOrNull(childCallName: String): Pair<KtExpression, KtFunctionLiteral>? {
        val callee = getChildCallOrNull(childCallName) ?: return null
        val lambda = callee.functionLiteralArgumentOrNull() ?: return null
        return callee.calleeExpression!! to lambda
    }

    private fun KtElement.getGetByNameCallWithLambdaOrNull(): Pair<KtExpression, KtFunctionLiteral>? {
        val callee = descendantsOfType<KtCallExpression>().firstOrNull {
            it.calleeExpression?.text == "getByName" &&
                it.valueArguments.size == 2 &&
                it.valueArguments[0].isReleaseBuildType()
        } ?: return null
        val lambda = callee.functionLiteralArgumentOrNull() ?: return null
        return callee.calleeExpression!! to lambda
    }

    private fun KtValueArgument.isReleaseBuildType(): Boolean =
        releaseBuildTypeExpressions.any(this::textMatches) // TODO: use predictRuntimeStringValue after migration to K2

    private fun KtCallExpression.functionLiteralArgumentOrNull(): KtFunctionLiteral? =
        valueArguments
            .flatMap { it.childrenOfType<KtLambdaExpression>() }
            .flatMap { it.childrenOfType<KtFunctionLiteral>() }
            .singleOrNull()

    private fun KtElement.getPropertyAssignmentOrNull(propertyName: String): KtBinaryExpression? =
        descendantsOfType<KtBinaryExpression>().firstOrNull { it.operationToken == KtTokens.EQ && it.left?.text == propertyName }
}
