/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.js.descriptorUtils.getKotlinTypeFqName
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.SecondaryLocation
import org.sonarsource.kotlin.api.suspendModifier
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private const val COROUTINE_SCOPE = "kotlinx.coroutines.CoroutineScope"
private const val MESSAGE = "Extension functions on CoroutineScope should not be suspending."

@Rule(key = "S6312")
class CoroutineScopeFunSuspendingCheck : AbstractCheck() {
    override fun visitNamedFunction(function: KtNamedFunction, kotlinFileContext: KotlinFileContext) {
        // Only applicable for suspending extension functions
        val suspendModifier = function.suspendModifier() ?: return
        val receiverType = function.receiverTypeReference ?: return
        val resolvedReceiverType = kotlinFileContext.bindingContext[BindingContext.TYPE, receiverType] ?: return

        if (
            (resolvedReceiverType.getKotlinTypeFqName(false) == COROUTINE_SCOPE ||
                resolvedReceiverType.supertypes().any { it.getKotlinTypeFqName(false) == COROUTINE_SCOPE })
        ) {
            val secondaries = listOf(SecondaryLocation(kotlinFileContext.textRange(receiverType)))
            kotlinFileContext.reportIssue(suspendModifier, MESSAGE, secondaries)
        }
    }
}
