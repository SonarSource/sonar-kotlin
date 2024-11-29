/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.resolve.BindingContext
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.annotatedElement
import org.sonarsource.kotlin.api.frontend.KotlinFileContext

@org.sonarsource.kotlin.api.frontend.K1only
@Rule(key = "S1133")
class DeprecatedCodeCheck : AbstractCheck() {
    
    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry, context: KotlinFileContext) {
        val descriptor = context.bindingContext[BindingContext.ANNOTATION, annotationEntry]
        if ("kotlin.Deprecated" == descriptor?.fqName?.asString()) {
            context.reportIssue(annotationEntry.elementToReport(), "Do not forget to remove this deprecated code someday.")
        }
    }
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
