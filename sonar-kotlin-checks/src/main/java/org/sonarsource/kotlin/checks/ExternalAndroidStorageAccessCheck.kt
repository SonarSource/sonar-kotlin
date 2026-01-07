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

import org.jetbrains.kotlin.analysis.api.resolution.KaCallableMemberCall
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.resolution.successfulCallOrNull
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

private const val MESSAGE = "Make sure accessing the Android external storage is safe here."

private val HOTSPOT_FUNS = listOf(
    FunMatcher(definingSupertype = "android.os.Environment") {
        withNames(
            "getExternalStorageDirectory",
            "getExternalStoragePublicDirectory"
        )
    },
    FunMatcher(definingSupertype = "android.content.Context") {
        withNames(
            "getExternalFilesDir",
            "getExternalFilesDirs",
            "getExternalCacheDir",
            "getExternalCacheDirs",
            "getExternalMediaDirs",
            "getObbDir",
            "getObbDirs",
        )
    }
)

private val HOTSPOT_PROPS = listOf(
    "externalCacheDir",
    "externalCacheDirs",
    "externalMediaDirs",
    "obbDir",
    "obbDirs",
)

@Rule(key = "S5324")
class ExternalAndroidStorageAccessCheck : CallAbstractCheck() {
    override val functionsToVisit = HOTSPOT_FUNS

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: KaFunctionCall<*>, kotlinFileContext: KotlinFileContext) {
        kotlinFileContext.reportIssue(callExpression.calleeExpression!!, MESSAGE)
    }

    override fun visitReferenceExpression(expression: KtReferenceExpression, kotlinFileContext: KotlinFileContext) = withKaSession {
        if (expression is KtNameReferenceExpression && expression.getReferencedName() in HOTSPOT_PROPS) {
            if (HOTSPOT_FUNS.any { it.matches(expression.resolveToCall()?.successfulCallOrNull<KaCallableMemberCall<*, *>>()) }) {
                kotlinFileContext.reportIssue(expression, MESSAGE)
            }
        }
    }
}
