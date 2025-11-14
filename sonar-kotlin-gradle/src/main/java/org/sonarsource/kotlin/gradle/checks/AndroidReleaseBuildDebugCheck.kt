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

private const val message = "Make sure this debug feature is deactivated before delivering the code in production."

@Rule(key = "S7416")
class AndroidReleaseBuildDebugCheck : AbstractCheck() {

    override fun visitScriptInitializer(initializer: KtScriptInitializer, data: KotlinFileContext) {
        !data.ktFile.isSettingGradleKts() || return
        val androidLambda = initializer.getChildCallWithLambdaOrNull("android")?.lambda ?: return

        // Ensure the project is an Android app, and not a library
        androidLambda.getApplicationId() ?: return

        val buildTypes = androidLambda.getChildCallWithLambdaOrNull("buildTypes") ?: return
        val (_, releaseLambda) = buildTypes.lambda.getChildCallWithLambdaOrNull("release")
            ?: androidLambda.getGetByNameCallWithLambdaOrNull()
            ?: return

        val isDebuggableAssignment = releaseLambda.getPropertyAssignmentOrNull("isDebuggable")

        if (isDebuggableAssignment?.right?.predictRuntimeBooleanValue() == true) {
            data.reportIssue(isDebuggableAssignment, message)
        }
    }
}
