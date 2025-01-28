/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.analysis.api.resolution.KaFunctionCall
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.CallAbstractCheck
import org.sonarsource.kotlin.api.checks.FUNS_ACCEPTING_DISPATCHERS
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.checks.FunMatcherImpl
import org.sonarsource.kotlin.api.checks.KOTLINX_COROUTINES_PACKAGE
import org.sonarsource.kotlin.api.checks.asFqNameString
import org.sonarsource.kotlin.api.checks.determineTypeAsString
import org.sonarsource.kotlin.api.checks.predictReceiverExpression
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

private val JOB_CONSTRUCTOR = FunMatcher(qualifier = KOTLINX_COROUTINES_PACKAGE, name = "Job")
private val SUPERVISOR_JOB_CONSTRUCTOR = FunMatcher(qualifier = KOTLINX_COROUTINES_PACKAGE, name = "SupervisorJob")
private const val MESSAGE_ENDING = " here leads to the breaking of structured concurrency principles."
private const val CLASS_TYPE = "kotlin.reflect.KClass"
private const val DELICATE_API_TYPE = "kotlinx.coroutines.DelicateCoroutinesApi"

@Rule(key = "S6306")
class StructuredConcurrencyPrinciplesCheck : CallAbstractCheck() {

    override val functionsToVisit = FUNS_ACCEPTING_DISPATCHERS

    override fun visitFunctionCall(
        callExpression: KtCallExpression,
        resolvedCall: KaFunctionCall<*>,
        matchedFun: FunMatcherImpl,
        kotlinFileContext: KotlinFileContext
    ) {
        val receiver = callExpression.predictReceiverExpression() as? KtNameReferenceExpression
        if (receiver?.getReferencedName() == "GlobalScope" && !callExpression.checkOptInDelicateApi()) {
            kotlinFileContext.reportIssue(receiver, """Using "GlobalScope"$MESSAGE_ENDING""")
        } else {
            val args = resolvedCall.argumentMapping.keys.toList()
            if (args.isEmpty()) return
            val argExprCall = args[0] as? KtCallExpression ?: return
            if (JOB_CONSTRUCTOR.matches(argExprCall) || SUPERVISOR_JOB_CONSTRUCTOR.matches(argExprCall)
            ) {
                kotlinFileContext.reportIssue(argExprCall, """Using "${argExprCall.text}"$MESSAGE_ENDING""")
            }
        }
    }
}

private fun KtExpression.checkOptInDelicateApi(): Boolean {
    var parent: PsiElement? = this
    while (parent != null) {
        val annotations = (parent as? KtAnnotated)?.annotationEntries
        if (annotations.isAnnotatedWithOptInDelicateApi()) return true
        parent = parent.parent
    }
    return false
}

private fun MutableList<KtAnnotationEntry>?.isAnnotatedWithOptInDelicateApi() = withKaSession {
    this@isAnnotatedWithOptInDelicateApi?.let { it ->
        it.any { annotation ->
            val typeFqn = annotation.typeReference?.determineTypeAsString()
            typeFqn == "kotlinx.coroutines.DelicateCoroutinesApi" ||
                (typeFqn == "kotlin.OptIn"
                    && annotation.valueArguments.any { valueArgument ->

                    val expressionType = valueArgument.getArgumentExpression()?.expressionType
                    val asFqNameString = expressionType?.asFqNameString()
                    asFqNameString == CLASS_TYPE && (expressionType as? KaClassType)?.typeArguments?.any {
                        it.type?.symbol?.classId?.asFqNameString() == DELICATE_API_TYPE
                    } ?: false
                })
        }
    } ?: false
}
