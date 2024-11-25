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

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.unwrappedGetMethod
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

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

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: ResolvedCall<*>, kotlinFileContext: KotlinFileContext) {
        kotlinFileContext.reportIssue(callExpression.calleeExpression!!, MESSAGE)
    }

    override fun visitReferenceExpression(expression: KtReferenceExpression, kotlinFileContext: KotlinFileContext) {
        if (expression is KtNameReferenceExpression && expression.getReferencedName() in HOTSPOT_PROPS) {
            val prop = kotlinFileContext.bindingContext[BindingContext.REFERENCE_TARGET, expression] as? PropertyDescriptor
            if (prop != null && HOTSPOT_FUNS.any { it.matches(prop.unwrappedGetMethod) }) {
                kotlinFileContext.reportIssue(expression, MESSAGE)
            }
        }
    }
}
