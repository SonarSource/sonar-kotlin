/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2026 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1.0.1, as published by SonarSource Sàrl.
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

import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.withKaSession

@Rule(key = "S6517")
class InterfaceCouldBeFunctionalCheck : AbstractCheck() {
    private val functionalInterClassId = ClassId.fromString("java/lang/FunctionalInterface")

    override fun visitClass(klass: KtClass, context: KotlinFileContext) {
        checkFunctionalInterface(klass, context)
        checkFunctionalInterfaceAnnotation(klass, context)
    }

    private fun checkFunctionalInterface(klass: KtClass, context: KotlinFileContext) {
        val isNonFunctionalInterface = klass.isInterface() && klass.getFunKeyword() == null
        if (isNonFunctionalInterface && hasExactlyOneFunctionAndNoProperties(klass) && !klass.isSealed()) {
            context.reportIssue(
                // As an interface has always a keyword, the not-null assertion is safe here.
                klass.getClassOrInterfaceKeyword()!!,
                "Make this interface functional or replace it with a function type."
            )
        }
    }

    private fun checkFunctionalInterfaceAnnotation(klass: KtClass, context: KotlinFileContext) = withKaSession {
        klass.annotationEntries.forEach {
            if (it.typeReference?.type?.isClassType(functionalInterClassId) == true) {
                context.reportIssue(it, """"@FunctionalInterface" annotation has no effect in Kotlin""")
            }
        }
    }
}

private fun hasExactlyOneFunctionAndNoProperties(klass: KtClass): Boolean {
    if (klass.superTypeListEntries.isNotEmpty()) return false
    var functionCount = 0
    for (declaration in klass.declarations) {
        if (declaration is KtProperty) return false
        if (declaration is KtNamedFunction) {
            if (
                functionCount > 0 ||
                declaration.hasTypeParameterListBeforeFunctionName() ||
                declaration.valueParameters.any { it.hasDefaultValue() }
            )
                return false
            functionCount++
        }
    }
    return functionCount > 0
}
