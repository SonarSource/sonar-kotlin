package org.sonarsource.kotlin.checks

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.isPublic
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.FUNCTION
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.suspendModifier
import org.sonarsource.kotlin.plugin.KotlinFileContext

@Rule(key = "S6313")
class ViewModelSuspendingFunctionsCheck : AbstractCheck() {

    override fun visitNamedFunction(function: KtNamedFunction, kotlinFileContext: KotlinFileContext) {
        val bindingContext = kotlinFileContext.bindingContext
        if (function.isPublic
            && function.suspendModifier() != null
            && function.extendsViewModel(bindingContext)
        ) {
            kotlinFileContext.reportIssue(function.nameIdentifier!!,
                """Classes extending "ViewModel" should not expose suspending functions.""")
        }
    }

    private fun KtNamedFunction.extendsViewModel(bindingContext: BindingContext): Boolean {
        val classDescriptor = bindingContext.get(FUNCTION, this)?.containingDeclaration as? ClassDescriptor
        return classDescriptor?.getAllSuperClassifiers()?.any {
            it.fqNameOrNull()?.asString() == "androidx.lifecycle.ViewModel"
        } ?: false
    }
}
