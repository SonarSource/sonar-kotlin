/*
 * SonarSource Kotlin
 * Copyright (C) SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * You can redistribute and/or modify this program under the terms of
 * the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.annotatedElement
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

@Rule(key = "S1133")
class DeprecatedCodeCheck : AbstractCheck() {

    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry, context: KotlinFileContext) = withKaSession {
        if (annotationEntry.typeReference?.type?.isClassType(StandardClassIds.Annotations.Deprecated) == true
            && !annotationEntry.isOnOverriddenElement()
            && !annotationEntry.hasNonWarningDeprecationLevel()
        ) {
            context.reportIssue(annotationEntry.elementToReport(), "Do not forget to remove this deprecated code someday.")
        }
    }
}

private fun KtAnnotationEntry.isOnOverriddenElement(): Boolean {
    val owner = annotatedElement()
    return owner is KtModifierListOwner && owner.hasModifier(KtTokens.OVERRIDE_KEYWORD)
}

private fun KtAnnotationEntry.hasNonWarningDeprecationLevel(): Boolean {
    val levelArg = valueArguments.find { it.getArgumentName()?.asName?.asString() == "level" }
        ?: return false
    val text = levelArg.getArgumentExpression()?.text ?: return false
    return text.endsWith("HIDDEN") || text.endsWith("ERROR")
}

private fun KtAnnotationEntry.elementToReport(): PsiElement =
    when (val annotated = annotatedElement()) {
        // Deprecated Primary constructor should always have a "constructor" keyword
        is KtPrimaryConstructor -> annotated.getConstructorKeyword()!!
        is KtSecondaryConstructor -> annotated.getConstructorKeyword()
        is KtPropertyAccessor -> annotated.namePlaceholder
        // Can deprecate anonymous functions and classes
        is KtNamedDeclaration -> annotated.nameIdentifier ?: this
        else -> this
    }
