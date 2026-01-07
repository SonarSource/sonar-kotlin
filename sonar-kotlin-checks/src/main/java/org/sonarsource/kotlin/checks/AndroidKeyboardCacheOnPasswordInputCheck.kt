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

import com.intellij.psi.util.descendantsOfType
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.KaSimpleVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.successfulVariableAccessCall
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.ConstructorMatcher
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.visiting.withKaSession

private const val WITH_KEYBOARD_OPTIONS_MESSAGE = """Set the "keyboardType" to "KeyboardType.Password" to disable the keyboard cache."""
private const val WITHOUT_KEYBOARD_OPTIONS_MESSAGE = """Set "keyboardOptions" to disable the keyboard cache."""

private val keyboardOptionsCompanionClassId = ClassId.fromString("androidx/compose/foundation/text/KeyboardOptions.Companion")
private val keyboardTypeCompanionClassId = ClassId.fromString("androidx/compose/ui/text/input/KeyboardType.Companion")

private val visualTransformationParamName = Name.identifier("visualTransformation")
private val keyboardOptionsParamName = Name.identifier("keyboardOptions")
private val keyboardTypeParamName = Name.identifier("keyboardType")

private val cacheEnabledKeyboardTypes = setOf("Ascii", "Decimal", "Email", "Number", "Phone", "Text", "Unspecified", "Uri")

private val textFieldsFunMatcher = listOf(
    FunMatcher {
        qualifiers = setOf(
            "androidx.compose.material",
            "androidx.compose.material2",
            "androidx.compose.material3",
        )
        names = setOf("TextField", "OutlinedTextField")
    },
)
private val passwordVisualTransformationFunMatcher = ConstructorMatcher(
    "androidx.compose.ui.text.input.PasswordVisualTransformation",
)
private val keyboardOptionsConstructorFunMatcher = ConstructorMatcher(
    "androidx.compose.foundation.text.KeyboardOptions",
)
private val keyboardOptionsCopyFunMatcher = FunMatcher {
    qualifier = "androidx.compose.foundation.text.KeyboardOptions"
    name = "copy"
}

@Rule(key = "S7410")
class AndroidKeyboardCacheOnPasswordInputCheck : CallAbstractCheck() {

    override val functionsToVisit = textFieldsFunMatcher

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        kotlinFileContext: KotlinFileContext,
    ) = withKaSession {
        val passwordVisualTransformationConstructor = resolvedCall.getPasswordVisualTransformationConstructorOrNull() ?: return@withKaSession
        val secondaryLocations = listOf(
            SecondaryLocation(kotlinFileContext.textRange(passwordVisualTransformationConstructor), ""),
        )
        val textFieldConstructor = callExpression.calleeExpression ?: return@withKaSession
        val keyboardOptionsArgument = resolvedCall.argumentMapping.entries.singleOrNull {
            it.value.symbol.name == keyboardOptionsParamName
        }?.key

        if (keyboardOptionsArgument == null) {
            kotlinFileContext.reportIssue(
                textFieldConstructor,
                WITHOUT_KEYBOARD_OPTIONS_MESSAGE,
                secondaryLocations = secondaryLocations,
            )
        } else if (isKeyboardOptionsWithCacheEnabled(keyboardOptionsArgument)) {
            kotlinFileContext.reportIssue(
                keyboardOptionsArgument,
                WITH_KEYBOARD_OPTIONS_MESSAGE,
                secondaryLocations = secondaryLocations,
            )
        }
    }

    private fun KaFunctionCall<*>.getPasswordVisualTransformationConstructorOrNull() : KtExpression? =
        argumentMapping.entries.singleOrNull { it.value.symbol.name == visualTransformationParamName }
            ?.key
            ?.descendantsOfType<KtCallExpression>()
            ?.firstOrNull(passwordVisualTransformationFunMatcher::matches)
            ?.calleeExpression

    private fun isKeyboardOptionsWithCacheEnabled(expression: KtExpression) : Boolean = withKaSession {
        return when (expression) {
            is KtDotQualifiedExpression -> isKeyboardOptionsWithCacheEnabledDotQualifiedExpression(expression)
            is KtCallExpression -> isKeyboardOptionsWithCacheEnabledCallExpression(expression)
            is KtReferenceExpression -> isKeyboardOptionsWithCacheEnabledReferenceExpression(expression)
            else -> false
        }
    }

    private fun isKeyboardOptionsWithCacheEnabledDotQualifiedExpression(expression: KtDotQualifiedExpression) : Boolean = withKaSession {
        return when {
            // e.g. KeyboardOptions.Default
            expression.receiverExpression.expressionType?.isClassType(keyboardOptionsCompanionClassId) == true -> {
                expression.selectorExpression?.text == "Default"
            }
            // e.g. keyboardOptions.copy(keyboardType = KeyboardType.Ascii)
            // no keyboardType argument means taking target copy value, that may or may not be cache enabled -> false
            expression.selectorExpression is KtCallExpression -> {
                val call = expression.selectorExpression as KtCallExpression
                keyboardOptionsCopyFunMatcher.matches(call) && (isCacheEnabledKeyboardTypeOrNull(call) ?: false)
            }
            else -> false
        }
    }

    private fun isKeyboardOptionsWithCacheEnabledCallExpression(expression: KtCallExpression) : Boolean = withKaSession {
        // e.g. KeyboardOptions(keyboardType = KeyboardType.Ascii)
        // no keyboardType argument means KeyboardType.Unspecified, that is cache enabled
        keyboardOptionsConstructorFunMatcher.matches(expression) && (isCacheEnabledKeyboardTypeOrNull(expression) ?: true)
    }

    private fun isKeyboardOptionsWithCacheEnabledReferenceExpression(expression: KtReferenceExpression): Boolean = withKaSession {
        val variableAccessCall = expression.resolveToCall()?.successfulVariableAccessCall() ?: return@withKaSession false
        val symbol = (variableAccessCall as? KaSimpleVariableAccessCall)?.symbol ?: return@withKaSession false
        val value = (symbol.psi as? KtProperty)?.takeIf { !it.isVar }?.initializer
            ?: (symbol.psi as? KtParameter)?.defaultValue
            ?: return@withKaSession false
        isKeyboardOptionsWithCacheEnabled(value)
    }

    private fun isCacheEnabledKeyboardTypeOrNull(expression: KtCallExpression) : Boolean? = withKaSession {
        expression.resolveToCall()
            ?.successfulFunctionCallOrNull() // Can be constructor or normal function call
            ?.argumentMapping
            ?.entries
            ?.singleOrNull { it.value.symbol.name == keyboardTypeParamName }
            ?.key
            ?.let(::isCacheEnabledKeyboardType)
    }

    private fun isCacheEnabledKeyboardType(expression: KtExpression): Boolean = withKaSession {
        expression is KtDotQualifiedExpression &&
            expression.receiverExpression.expressionType?.isClassType(keyboardTypeCompanionClassId) == true &&
            cacheEnabledKeyboardTypes.contains(expression.selectorExpression?.text)
    }
}
