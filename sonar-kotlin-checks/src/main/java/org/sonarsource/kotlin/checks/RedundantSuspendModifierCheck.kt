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

import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.overrides
import org.sonarsource.kotlin.api.checks.suspendModifier
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@Rule(key = "S6318")
class RedundantSuspendModifierCheck : AbstractCheck() {

    override fun visitNamedFunction(function: KtNamedFunction, context: KotlinFileContext) {
        val suspendModifier = function.suspendModifier() ?: return
        with(function) {
            if (hasBody() && !overrides()) {
                findDescendantOfType<KtNameReferenceExpression> {
                    it.getResolvedCall(context.bindingContext)?.resultingDescriptor?.isSuspend ?: true
                } ?: context.reportIssue(suspendModifier, """Remove this unnecessary "suspend" modifier.""")
            }
        }
    }
}
