package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi2ir.unwrappedGetMethod
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.CallAbstractCheck
import org.sonarsource.kotlin.api.FunMatcher
import org.sonarsource.kotlin.plugin.KotlinFileContext

private const val MESSAGE = "Accessing Android external storage is security-sensitive"

private val HOTSPOT_FUNS = listOf(
    FunMatcher(qualifier = "android.os.Environment") {
        withNames(
            "getExternalStorageDirectory",
            "getExternalStoragePublicDirectory"
        )
    },
    FunMatcher(qualifier = "android.content.Context") {
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
    override fun functionsToVisit() = HOTSPOT_FUNS

    override fun visitFunctionCall(callExpression: KtCallExpression, resolvedCall: ResolvedCall<*>, kotlinFileContext: KotlinFileContext) {
        val calleeExpression = callExpression.calleeExpression ?: return
        kotlinFileContext.reportIssue(calleeExpression, MESSAGE)
    }

    override fun visitReferenceExpression(expression: KtReferenceExpression, kotlinFileContext: KotlinFileContext) {
        if (expression is KtNameReferenceExpression && expression.getReferencedName() in HOTSPOT_PROPS) {
            val prop = kotlinFileContext.bindingContext.get(BindingContext.REFERENCE_TARGET, expression) as? PropertyDescriptor
            if (prop != null && HOTSPOT_FUNS.any { it.matches(prop.unwrappedGetMethod) }) {
                kotlinFileContext.reportIssue(expression, MESSAGE)
            }
        }
    }
}
