/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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

import org.jetbrains.kotlin.psi.KtNamedFunction
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.COROUTINES_CHANNEL
import org.sonarsource.kotlin.api.checks.COROUTINES_FLOW
import org.sonarsource.kotlin.api.reporting.SecondaryLocation
import org.sonarsource.kotlin.api.checks.returnTypeAsString
import org.sonarsource.kotlin.api.checks.suspendModifier
import org.sonarsource.kotlin.api.reporting.KotlinTextRanges.textRange
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

private val FORBIDDEN_RETURN_TYPES = listOf(COROUTINES_FLOW, COROUTINES_CHANNEL)
private const val MESSAGE = """Functions returning "Flow" or "Channel" should not be suspending"""

@Rule(key = "S6309")
class FlowChannelReturningFunsNotSuspendingCheck : AbstractCheck() {

    override fun visitNamedFunction(function: KtNamedFunction, kotlinFileContext: KotlinFileContext) {
        function.suspendModifier()?.let { suspendModifier ->
            if (function.returnTypeAsString(kotlinFileContext.bindingContext) in FORBIDDEN_RETURN_TYPES) {
                val secondaries = function.typeReference
                    ?.let { listOf(SecondaryLocation(kotlinFileContext.textRange(it))) }
                    ?: emptyList()
                kotlinFileContext.reportIssue(suspendModifier, MESSAGE, secondaries)
            }
        }
    }
}
