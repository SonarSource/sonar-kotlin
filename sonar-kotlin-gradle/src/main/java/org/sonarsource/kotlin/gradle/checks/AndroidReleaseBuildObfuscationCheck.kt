/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import org.jetbrains.kotlin.psi.KtScriptInitializer
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.predictRuntimeBooleanValue
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.reporting.SecondaryLocation

private const val mainMessage = "Make sure that obfuscation is enabled in the release build configuration."
private const val debuggableSetToTrueMessage = "Enabling debugging disables obfuscation for this release build. Make sure this is safe here."

@Rule(key = "S7204")
class AndroidReleaseBuildObfuscationCheck : AbstractCheck() {

    override fun visitScriptInitializer(initializer: KtScriptInitializer, data: KotlinFileContext) {
        !data.ktFile.isSettingGradleKts() || return
        val (androidCallee, androidLambda) = initializer.getChildCallWithLambdaOrNull("android") ?: return

        // Ensure the project is an Android app, and not a library
        androidLambda.getApplicationId() ?: return

        val buildTypes = androidLambda.getChildCallWithLambdaOrNull("buildTypes")
        if (buildTypes == null) {
            data.reportIssue(androidCallee, mainMessage)
            return
        }

        val (releaseCallee, releaseLambda) = buildTypes.lambda.getChildCallWithLambdaOrNull("release")
            ?: androidLambda.getGetByNameCallWithLambdaOrNull()
            ?: return

        val isDebuggableAssignment = releaseLambda.getPropertyAssignmentOrNull("isDebuggable")
        val isMinifiedEnabledAssignment = releaseLambda.getPropertyAssignmentOrNull("isMinifyEnabled")

        when {
            isMinifiedEnabledAssignment == null ->
                data.reportIssue(releaseCallee, mainMessage)
            isMinifiedEnabledAssignment.right?.predictRuntimeBooleanValue() == false ->
                data.reportIssue(isMinifiedEnabledAssignment, mainMessage)
            isDebuggableAssignment?.right?.predictRuntimeBooleanValue() == true ->
                data.reportIssue(
                    releaseCallee,
                    debuggableSetToTrueMessage,
                    secondaryLocations = listOf(isDebuggableAssignment).map { SecondaryLocation(data.textRange(it), "") }
                )
            releaseLambda.getChildCallOrNull("proguardFiles") == null ->
                data.reportIssue(releaseCallee, mainMessage)
        }
    }
}
