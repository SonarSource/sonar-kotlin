/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2022 SonarSource SA
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

import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.calls.util.getCall
import org.jetbrains.kotlin.resolve.calls.util.getFirstArgumentExpression
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.*
import org.sonarsource.kotlin.plugin.KotlinFileContext

val fileOutputStreamConstructorMatcher = ConstructorMatcher(typeName = "java.io.FileOutputStream") {
    withArguments("kotlin.String", "kotlin.Boolean")
    withArguments("java.io.File", "kotlin.Boolean")
}

val filesNewOutputStreamMatcher = FunMatcher(qualifier = "java.nio.file.Files") {
    withArguments(
        ArgumentMatcher(typeName = "java.nio.file.Path"),
        ArgumentMatcher(typeName = "java.nio.file.OpenOption", isVararg = true),

        )
}

const val APPEND = "java.nio.file.StandardOpenOption.APPEND"

@Rule(key = "S2689")
class ObjectOutputStreamCheck : CallAbstractCheck() {
    override val functionsToVisit: Iterable<FunMatcherImpl> =
        listOf(ConstructorMatcher(typeName = "java.io.ObjectOutputStream") {
            withArguments("java.io.OutputStream")
        })

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: ResolvedCall<*>,
        kotlinFileContext: KotlinFileContext
    ) {
        val bindingContext = kotlinFileContext.bindingContext
        callExpression.getResolvedCall(bindingContext)?.getFirstArgumentExpression()
            ?.let { arg ->
                arg.predictRuntimeValueExpression(bindingContext).getCall(bindingContext)
                    ?.also { call ->
                        if (fileOutputStreamConstructorMatcher.matches(call, bindingContext)) {
                            (call.getResolvedCall(bindingContext)?.valueArgumentsByIndex?.get(1) as? ExpressionValueArgument)
                                ?.valueArgument?.getArgumentExpression()?.predictRuntimeBooleanValue(bindingContext)
                                ?.ifTrue {
                                    kotlinFileContext.reportIssue(
                                        callExpression.calleeExpression!!,
                                        "Do not use a FileOutputStream in append mode."
                                    )
                                }
                        } else if (filesNewOutputStreamMatcher.matches(call, bindingContext)) {

                            val varargValueArgument =
                                call.getResolvedCall(bindingContext)?.valueArgumentsByIndex?.get(1) as? VarargValueArgument ?: return
                            varargValueArgument.arguments.any {
                                (it.getArgumentExpression()?.predictRuntimeValueExpression(bindingContext) as? KtDotQualifiedExpression)
                                    ?.resolveReferenceTarget(bindingContext).determineType()?.getJetTypeFqName(false) == APPEND
                            }.ifTrue {
                                kotlinFileContext.reportIssue(
                                    callExpression.calleeExpression!!,
                                    "Do not use a FileOutputStream in append mode."
                                )
                            }
                        }
                    }
            }
    }

}
