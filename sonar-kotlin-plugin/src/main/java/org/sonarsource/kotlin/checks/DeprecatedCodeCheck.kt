/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2021 SonarSource SA
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

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.resolve.BindingContext
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinFileContext

@Rule(key = "S1133")
class DeprecatedCodeCheck : AbstractCheck() {
    
    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry, context: KotlinFileContext) {
        val descriptor = context.bindingContext.get(BindingContext.ANNOTATION, annotationEntry)
        if ("kotlin.Deprecated" == descriptor?.fqName?.asString()) {
            context.reportIssue(annotationEntry.elementToReport(), "Do not forget to remove this deprecated code someday.")
        }
    }
}

private fun KtAnnotationEntry.elementToReport(): PsiElement =
    // The first parent is always a KtDeclarationModifierList and the second parent is always an annotated entity.
    // If for some reason this assumption is broken, we report on the annotation itself.
    when (val parent = parent.parent) {
        is KtSecondaryConstructor -> parent.getConstructorKeyword()
        is KtPropertyAccessor -> parent.namePlaceholder
        // Cannot deprecate entity without a name
        is KtNamedDeclaration -> parent.nameIdentifier!!
        else -> this
    }
