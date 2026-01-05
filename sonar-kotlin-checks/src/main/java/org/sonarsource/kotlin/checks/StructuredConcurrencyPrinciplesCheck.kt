/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource SA
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
import org.jetbrains.kotlin.name.ClassId
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
import org.sonarsource.kotlin.api.checks.predictReceiverExpression
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

private val JOB_CONSTRUCTOR = FunMatcher(qualifier = KOTLINX_COROUTINES_PACKAGE, name = "Job")
private val SUPERVISOR_JOB_CONSTRUCTOR = FunMatcher(qualifier = KOTLINX_COROUTINES_PACKAGE, name = "SupervisorJob")
private const val MESSAGE_ENDING = " here leads to the breaking of structured concurrency principles."
private val DELICATE_COROUTINES_API_CLASS_ID = ClassId.fromString("kotlinx/coroutines/DelicateCoroutinesApi")
private val KCLASS_CLASS_ID = ClassId.fromString("kotlin/reflect/KClass")
private val OPTIN_CLASS_ID = ClassId.fromString("kotlin/OptIn")

@Rule(key = "S6306")
class StructuredConcurrencyPrinciplesCheck : CallAbstractCheck() {

    override val functionsToVisit = FUNS_ACCEPTING_DISPATCHERS

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: KaFunctionCall<*>, matchedFun: FunMatcherImpl, kotlinFileContext: KotlinFileContext) {
        val receiver = callExpression.predictReceiverExpression() as? KtNameReferenceExpression
        if (receiver?.getReferencedName() == "GlobalScope" && !callExpression.checkOptInDelicateApi()) {
            kotlinFileContext.reportIssue(receiver, """Using "GlobalScope"$MESSAGE_ENDING""")
        } else {
            val argExprCall = resolvedCall.argumentMapping.keys.firstOrNull() as? KtCallExpression ?: return
            if (JOB_CONSTRUCTOR.matches(argExprCall) || SUPERVISOR_JOB_CONSTRUCTOR.matches(argExprCall)) {
                kotlinFileContext.reportIssue(argExprCall, """Using "${argExprCall.text}"$MESSAGE_ENDING""")
            }
        }
    }
}

/**
 * Unfortunately as of writing kotlin-analysis-api does not provide
 * [org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation]s
 * for [org.jetbrains.kotlin.psi.KtAnnotatedExpression].
 */
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
    this@isAnnotatedWithOptInDelicateApi?.let {
        it.any { annotation ->
            val annotationType = annotation.typeReference?.type
            annotationType != null && (annotationType.isClassType(DELICATE_COROUTINES_API_CLASS_ID) ||
                (annotationType.isClassType(OPTIN_CLASS_ID)
                    && annotation.valueArguments.any { valueArgument ->
                    val argumentType = valueArgument.getArgumentExpression()?.expressionType
                    argumentType != null
                            && argumentType.isClassType(KCLASS_CLASS_ID)
                            && (argumentType as KaClassType).typeArguments.any {
                        it.type?.isClassType(DELICATE_COROUTINES_API_CLASS_ID) == true
                    }
                }))
        }
    } ?: false
}
