package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.psi.psiUtil.isProtected
import org.jetbrains.kotlin.psi.psiUtil.isTopLevelKtOrJavaMember
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.determineTypeAsString
import org.sonarsource.kotlin.plugin.KotlinFileContext

private val DISALLOWED_TYPES = listOf(
    "kotlinx.coroutines.flow.MutableSharedFlow",
    "kotlinx.coroutines.flow.MutableStateFlow",
)

private const val MESSAGE = "Don't expose mutable flow types"

@Rule(key = "S6305")
class ExposedMutableFlowCheck : AbstractCheck() {
    override fun visitProperty(property: KtProperty, kotlinFileContext: KotlinFileContext) {
        if (isEligible(property) && property.determineTypeAsString(kotlinFileContext.bindingContext) in DISALLOWED_TYPES) {
            kotlinFileContext.reportIssue(property, MESSAGE)
        }
    }

    override fun visitParameter(parameter: KtParameter, kotlinFileContext: KotlinFileContext) {
        if (isEligible(parameter) && parameter.determineTypeAsString(kotlinFileContext.bindingContext) in DISALLOWED_TYPES) {
            kotlinFileContext.reportIssue(parameter, MESSAGE)
        }
    }
}

private fun isEligible(declaration: KtDeclaration) = !declaration.isProtected() && !declaration.isPrivate() &&
    (declaration.isTopLevelKtOrJavaMember() || declaration.containingClassOrObject != null)
