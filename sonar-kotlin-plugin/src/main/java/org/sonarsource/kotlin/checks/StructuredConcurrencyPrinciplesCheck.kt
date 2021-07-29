package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.TYPE
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.FUNS_ACCEPTING_DISPATCHERS
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.api.KOTLINX_COROUTINES_PACKAGE
import org.sonarsource.kotlin.api.determineTypeAsString
import org.sonarsource.kotlin.api.predictReceiverExpression
import org.sonarsource.kotlin.plugin.KotlinFileContext

private val JOB_CONSTRUCTOR = FunMatcher(qualifier = KOTLINX_COROUTINES_PACKAGE, name = "Job")
private val SUPERVISOR_JOB_CONSTRUCTOR = FunMatcher(qualifier = KOTLINX_COROUTINES_PACKAGE, name = "SupervisorJob")
private const val MESSAGE_ENDING = " here leads to breaking of structured concurrency principles."
private const val DELICATE_API_CLASS_TYPE = "kotlin.reflect.KClass<kotlinx.coroutines.DelicateCoroutinesApi>"

@Rule(key = "S6306")
class StructuredConcurrencyPrinciplesCheck : CallAbstractCheck() {

    override fun functionsToVisit() = FUNS_ACCEPTING_DISPATCHERS

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: ResolvedCall<*>, kotlinFileContext: KotlinFileContext) {
        val bindingContext = kotlinFileContext.bindingContext
        val receiver = callExpression.predictReceiverExpression(bindingContext) as? KtNameReferenceExpression
        if (receiver?.getReferencedName() == "GlobalScope" && !callExpression.checkOptInDelicateApi(bindingContext)) {
            kotlinFileContext.reportIssue(receiver, """Using "GlobalScope"$MESSAGE_ENDING""")
        } else {
            resolvedCall.valueArgumentsByIndex?.let { args ->
                if (args.isEmpty()) return
                val argExprCall = (args[0] as? ExpressionValueArgument)?.valueArgument?.getArgumentExpression() as? KtCallExpression ?: return
                if (JOB_CONSTRUCTOR.matches(argExprCall, bindingContext) || SUPERVISOR_JOB_CONSTRUCTOR.matches(argExprCall, bindingContext)
                ) {
                    kotlinFileContext.reportIssue(argExprCall, """Using "${argExprCall.text}"$MESSAGE_ENDING""")
                }
            }
        }
    }
}

private fun KtExpression.checkOptInDelicateApi(bindingContext: BindingContext): Boolean {
    var parent: PsiElement? = this
    while (parent != null) {
        val annotations = (parent as? KtAnnotated)?.annotationEntries
        if (annotations.isAnnotatedWithOptInDelicateApi(bindingContext)) return true
        parent = parent.parent
    }
    return false
}

private fun MutableList<KtAnnotationEntry>?.isAnnotatedWithOptInDelicateApi(bindingContext: BindingContext) =
    this?.let {
        it.any { annotation ->
            bindingContext.get(TYPE, annotation.typeReference)
            val typeFqn = annotation.typeReference?.determineTypeAsString(bindingContext)
            typeFqn == "kotlinx.coroutines.DelicateCoroutinesApi" ||
                (typeFqn == "kotlin.OptIn"
                    && annotation.valueArguments.any { valueArgument ->
                    valueArgument.getArgumentExpression()?.getType(bindingContext)
                        ?.getJetTypeFqName(true) == DELICATE_API_CLASS_TYPE
                })
        }
    } ?: false
