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

import org.jetbrains.kotlin.js.descriptorUtils.getJetTypeFqName
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.getType
import org.sonarsource.kotlin.plugin.KotlinFileContext

@Rule(key = "S6517")
class InterfaceCouldBeFunctionalCheck : AbstractCheck() {

    override fun visitClass(klass: KtClass, context: KotlinFileContext) {
        checkFunctionalInterface(klass, context)
        checkFunctionalInterfaceAnnotation(klass, context)
    }

    private fun checkFunctionalInterface(klass: KtClass, context: KotlinFileContext) {
        val isNonFunctionalInterface = klass.isInterface() && klass.getFunKeyword() == null
        if (isNonFunctionalInterface && hasOneFunctionAndNoProperties(klass)) {
            context.reportIssue(
                // An interface has always a keyword, so the not-null assertion is safe here.
                klass.getClassOrInterfaceKeyword()!!,
                "Interface should be functional or replaced with a functional type."
            )
        }
    }

    private fun checkFunctionalInterfaceAnnotation(klass: KtClass, context: KotlinFileContext) {
        klass.annotationEntries.forEach {
            if (isFunctionalInterfaceAnnotation(it, context)) {
                context.reportIssue(it, """"@FunctionalInterface" annotation has no effect in Kotlin""")
            }
        }
    }
}

private fun hasOneFunctionAndNoProperties(klass: KtClass): Boolean {
    // Note: other possible declarations are KtClass (classes, interfaces) and
    //       KtObjectDeclaration (companion objects), but they are allowed inside function interfaces
    var functionCount = 0
    return klass.declarations.all {
        it !is KtProperty && (it !is KtNamedFunction || functionCount++ == 0)
    } && functionCount > 0
}

private fun isFunctionalInterfaceAnnotation(annotation: KtAnnotationEntry, context: KotlinFileContext): Boolean {
    val annotationType = annotation.typeReference.getType(context.bindingContext)
    return (annotationType?.getJetTypeFqName(false) == "java.lang.FunctionalInterface")
}
