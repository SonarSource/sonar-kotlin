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

import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.allConstructors
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifier
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.kotlin.visiting.KtTreeVisitor

@Rule(key = "S6515")
class SingletonPatternCheck : AbstractCheck() {

    override fun visitKtFile(file: KtFile, kotlinFileContext: KotlinFileContext) {
        val singletonClassCandidateExtractor = SingletonClassCandidateExtractor()
        singletonClassCandidateExtractor.visitTree(file)
        val singletonClassCandidates = singletonClassCandidateExtractor.singletonClassCandidates
        if (singletonClassCandidates.isEmpty()) return

        val singleConstructorCallExtractor =
            SingleConstructorCallExtractor(singletonClassCandidates, kotlinFileContext.bindingContext)
        singleConstructorCallExtractor.visitTree(file)
        val singleConstructorCallByClass = singleConstructorCallExtractor.singleConstructorCallByClass
        if (singleConstructorCallByClass.isEmpty()) return

        singleConstructorCallByClass.values.stream().filter {
            isInitializingCall(it) || isLazyInitializingCall(it, kotlinFileContext.bindingContext)
        }.forEach {
            kotlinFileContext.reportIssue(it, "Singleton pattern should use object declarations or expressions")
        }
    }
}

private class SingletonClassCandidateExtractor : KtTreeVisitor() {

    val singletonClassCandidates = mutableSetOf<String>()

    override fun visitClass(klass: KtClass) {
        if (isSingletonClassCandidate(klass)) {
            klass.fqName?.let {
                singletonClassCandidates.add(it.toString())
            }
        }
    }
}

private fun isSingletonClassCandidate(klass: KtClass): Boolean {
    val constructors = klass.allConstructors
    return klass.isPrivate() || (constructors.isNotEmpty() && constructors.all(KtModifierListOwner::isPrivate))
}

private fun KtModifierListOwner.isPrivate(): Boolean = visibilityModifier()?.text == "private"

private class SingleConstructorCallExtractor(
    private val singletonClassCandidates: MutableSet<String>,
    private val bindingContext: BindingContext,
) : KtTreeVisitor() {

    val singleConstructorCallByClass: MutableMap<String, KtCallExpression> = mutableMapOf()

    override fun visitCallExpression(expression: KtCallExpression) {
        val fqName = getCalledConstructorClassFqNameOrNull(expression) ?: return
        if (!singletonClassCandidates.contains(fqName)) return
        if (singleConstructorCallByClass.put(fqName, expression) != null) {
            singleConstructorCallByClass.remove(fqName)
            singletonClassCandidates.remove(fqName)
        }
    }

    private fun getCalledConstructorClassFqNameOrNull(expression: KtCallExpression): String? {
        val calleeReference = expression.calleeExpression as? KtReferenceExpression ?: return null
        val calledConstructor = bindingContext[BindingContext.REFERENCE_TARGET, calleeReference] as? ConstructorDescriptor
        return calledConstructor?.constructedClass?.fqNameSafe?.toString()
    }
}

private fun isInitializingCall(callExpression: KtCallExpression): Boolean {
    val property = callExpression.parent as? KtProperty ?: return false
    return isDeclaredInCompanionObject(property)
}

private fun isLazyInitializingCall(callExpression: KtCallExpression, bindingContext: BindingContext): Boolean {
    val bodyExpression = callExpression.parent as? KtBlockExpression ?: return false
    if (bodyExpression.statements.last() != callExpression) return false
    val call = (((bodyExpression.parent as? KtFunctionLiteral)
        ?.parent as? KtLambdaExpression)
        ?.parent as? KtLambdaArgument)
        ?.parent as? KtCallExpression ?: return false

    val property = (call.parent as? KtPropertyDelegate)?.parent as? KtProperty ?: return false
    if (call.valueArguments.size != 1 || !isDeclaredInCompanionObject(property)) return false
    val calleeReference = call.calleeExpression as? KtReferenceExpression ?: return false
    return bindingContext[BindingContext.REFERENCE_TARGET, calleeReference]?.name?.toString() == "lazy"
}

private fun isDeclaredInCompanionObject(property: KtProperty): Boolean =
    ((property.parent as? KtClassBody)?.parent as? KtObjectDeclaration)?.isCompanion() ?: false
