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

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.jetbrains.kotlin.resolve.BindingContext
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.isInfix
import org.sonarsource.kotlin.plugin.KotlinFileContext

// Serializable method should not raise any issue in Kotlin.
private val IGNORED_METHODS: Set<String> = setOf(
    "writeObject",
    "readObject",
    "writeReplace",
    "readResolve",
    "readObjectNoData",
)

private val COMMON_ANNOTATIONS = listOf(
    "kotlin.OptIn", 
    "kotlin.Suppress",
    "kotlinx.coroutines.DelicateCoroutinesApi",
    "kotlin.jvm.Throws",
)

@Rule(key = "S1144")
class UnusedPrivateMethodCheck : AbstractCheck() {

    override fun visitClass(klass: KtClass, context: KotlinFileContext) {
        if (!klass.isTopLevel()) return
        klass.collectDescendantsOfType<KtNamedFunction> { it.shouldCheckForUsage(context.bindingContext) }
            .forEach {
                val functionName = it.name!!
                if (!IGNORED_METHODS.contains(functionName) && !it.isReferencedIn(klass, functionName)) {
                    // Anonymous functions can't be private, so nameIdentifier is always present
                    context.reportIssue(it.nameIdentifier!!, """Remove this unused private "$functionName" method.""")
                }
            }
    }

    private fun KtNamedFunction.isReferencedIn(klass: KtClass, name: String) =
        klass.hasReferences(name) || (isInfix() && klass.hasInfixReferences(name))

    private fun KtClass.hasReferences(name: String) =
        anyDescendantOfType<KtNameReferenceExpression> { it.getReferencedName() == name }

    private fun KtClass.hasInfixReferences(name: String) =
        anyDescendantOfType<KtOperationReferenceExpression> { it.getReferencedName() == name }

    private fun KtNamedFunction.shouldCheckForUsage(bindingContext: BindingContext) =
        isPrivate()
            && !hasModifier(KtTokens.OPERATOR_KEYWORD)
            && (annotationEntries.isEmpty() || annotatedWithCommonAnnotations(bindingContext))

    private fun KtNamedFunction.annotatedWithCommonAnnotations(bindingContext: BindingContext) =
        bindingContext.get(BindingContext.FUNCTION, this)
            ?.annotations
            ?.all { it.fqName?.asString() in COMMON_ANNOTATIONS }
            ?: false

}
