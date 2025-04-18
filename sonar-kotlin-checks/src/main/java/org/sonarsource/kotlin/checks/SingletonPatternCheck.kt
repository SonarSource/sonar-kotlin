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

import org.jetbrains.kotlin.analysis.api.resolution.successfulFunctionCallOrNull
import org.jetbrains.kotlin.analysis.api.resolution.symbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtLambdaArgument
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.psi.allConstructors
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.ConstructorMatcher
import org.sonarsource.kotlin.api.checks.FunMatcher
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.KtTreeVisitor
import org.sonarsource.kotlin.api.visiting.withKaSession

private val lazyInitializationMatcher = FunMatcher(
    name = "lazy",
    definingSupertype = "kotlin"
)

@Rule(key = "S6515")
class SingletonPatternCheck : AbstractCheck() {

    override fun visitKtFile(file: KtFile, kotlinFileContext: KotlinFileContext) {
        val singletonClassCandidateExtractor = SingletonClassCandidateExtractor()
        singletonClassCandidateExtractor.visitTree(file)
        val singletonClassCandidates = singletonClassCandidateExtractor.singletonClassCandidates
        if (singletonClassCandidates.isEmpty()) return

        val singleConstructorCallExtractor =
            SingleConstructorCallExtractor(singletonClassCandidates)
        singleConstructorCallExtractor.visitTree(file)
        val singleConstructorCallByClass = singleConstructorCallExtractor.singleConstructorCallByClass
        if (singleConstructorCallByClass.isEmpty()) return

        singleConstructorCallByClass.values.stream().filter {
            isInitializingCall(it) || isLazyInitializingCall(it)
        }.forEach {
            kotlinFileContext.reportIssue(it, "Singleton pattern should use object declarations or expressions.")
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
    return klass.isPrivate() || (constructors.isNotEmpty() && constructors.all { it.isPrivate() })
}

private class SingleConstructorCallExtractor(
    private val singletonClassCandidates: MutableSet<String>,
) : KtTreeVisitor() {

    val singleConstructorCallByClass: MutableMap<String, KtCallExpression> = mutableMapOf()

    private var constructorMatcher = ConstructorMatcher { withTypeNames(*singletonClassCandidates.toTypedArray()) }

    override fun visitCallExpression(expression: KtCallExpression): Unit = withKaSession {
        if (singletonClassCandidates.isEmpty() || !constructorMatcher.matches(expression)) return

        val kaConstructorSymbol = expression.resolveToCall()?.successfulFunctionCallOrNull()
            ?.partiallyAppliedSymbol?.symbol as? KaConstructorSymbol ?: return
        val fqName = kaConstructorSymbol.containingClassId?.asFqNameString() ?: return
        singleConstructorCallByClass.put(fqName, expression)?.let {
            singleConstructorCallByClass.remove(fqName)
            singletonClassCandidates.remove(fqName)
            constructorMatcher = ConstructorMatcher { withTypeNames(*singletonClassCandidates.toTypedArray()) }
        }
    }
}

private fun isInitializingCall(callExpression: KtCallExpression): Boolean {
    val property = callExpression.parent as? KtProperty ?: return false
    return isStaticProperty(property)
}

private fun isLazyInitializingCall(callExpression: KtCallExpression): Boolean {
    val bodyExpression = callExpression.parent as? KtBlockExpression ?: return false
    if (bodyExpression.statements.last() !== callExpression) return false
    val call = (((bodyExpression.parent as? KtFunctionLiteral)
        ?.parent as? KtLambdaExpression)
        ?.parent as? KtLambdaArgument)
        ?.parent as? KtCallExpression ?: return false

    val property = (call.parent as? KtPropertyDelegate)?.parent as? KtProperty ?: return false
    return isStaticProperty(property) && lazyInitializationMatcher.matches(call)
}

private fun isStaticProperty(property: KtProperty): Boolean =
    property.isTopLevel || ((property.parent as? KtClassBody)?.parent as? KtObjectDeclaration)?.isCompanion() ?: false
