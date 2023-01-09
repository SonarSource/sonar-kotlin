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

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
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
        function.suspendModifier()?.let {
            if (!function.isPrivate()
                && function.extendsViewModel(bindingContext)
            ) {
                kotlinFileContext.reportIssue(it,
                    """Classes extending "ViewModel" should not expose suspending functions.""")
            }
        }
    }
}

private fun KtNamedFunction.extendsViewModel(bindingContext: BindingContext): Boolean {
    val classDescriptor = bindingContext.get(FUNCTION, this)?.containingDeclaration as? ClassDescriptor
    return classDescriptor?.getAllSuperClassifiers()?.any {
        it.fqNameOrNull()?.asString() == "androidx.lifecycle.ViewModel"
    } ?: false
}
