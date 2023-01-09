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

import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.predictReceiverExpression
import org.sonarsource.kotlin.api.predictRuntimeValueExpression
import org.sonarsource.kotlin.plugin.KotlinFileContext

private val MUTABLE_COLLECTION_FUN_MATCHER = FunMatcher(definingSupertype = "kotlin.collections.MutableCollection") {
    withNames("containsAll", "addAll", "removeAll", "retainAll", "add")
}

private val COLLECTIONS_FUN_MATCHER = FunMatcher(definingSupertype = "kotlin.collections") {
    withNames("containsAll", "addAll", "removeAll", "retainAll", "fill")
}

private const val MESSAGE = "Collections should not be passed as arguments to their own methods."

@Rule(key = "S2114")
class CollectionCallingItselfCheck : CallAbstractCheck() {
    override val functionsToVisit = listOf(MUTABLE_COLLECTION_FUN_MATCHER, COLLECTIONS_FUN_MATCHER)

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: ResolvedCall<*>,
        kotlinFileContext: KotlinFileContext,
    ) {
        val bindingContext = kotlinFileContext.bindingContext

        val receiver = callExpression.predictReceiverExpression(bindingContext)?.predictRuntimeValueExpression(bindingContext) ?: return
        val argument = callExpression.valueArguments[0].getArgumentExpression()!!

        val argumentValue = argument.predictRuntimeValueExpression(bindingContext)

        if (receiver === argumentValue) {
            kotlinFileContext.reportIssue(argument, MESSAGE)
        }
    }
}
