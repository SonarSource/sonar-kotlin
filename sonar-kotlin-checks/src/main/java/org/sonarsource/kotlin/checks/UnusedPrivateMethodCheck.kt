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

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtOperationReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.anyDescendantOfType
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.isPrivate
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.isInfix
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

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
        klass.collectDescendantsOfType<KtNamedFunction> { it.shouldCheckForUsage() }
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

    private fun KtNamedFunction.shouldCheckForUsage() =
        isPrivate()
            && !hasModifier(KtTokens.OPERATOR_KEYWORD)
            && (annotationEntries.isEmpty() || annotatedWithCommonAnnotations())

    private fun KtNamedFunction.annotatedWithCommonAnnotations() = withKaSession {
        symbol.annotations.all {
            it.classId?.asFqNameString() in COMMON_ANNOTATIONS
        }
    }

}
