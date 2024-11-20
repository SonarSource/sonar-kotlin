/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.checks.merge
import org.sonarsource.kotlin.api.frontend.KotlinFileContext
import org.sonarsource.kotlin.api.visiting.analyze

@Rule(key = "S6516")
class SamConversionCheck : AbstractCheck() {

    @OptIn(KaExperimentalApi::class)
    override fun visitObjectDeclaration(declaration: KtObjectDeclaration, context: KotlinFileContext) {
        val superTypeEntry = declaration.superTypeListEntries.singleOrNull() ?: return

        analyze {
            val typeReference = superTypeEntry.typeReference ?: return
            if ((typeReference.type.isFunctionalInterface || typeReference.type.functionTypeKind != null) && declaration.hasExactlyOneFunctionAndNoProperties()) {
                val textRange = context.merge(declaration.getDeclarationKeyword()!!, superTypeEntry)
                context.reportIssue(textRange, "Replace explicit functional interface implementation with lambda expression.")
            }
        }
    }
}

private fun KtClassOrObject.hasExactlyOneFunctionAndNoProperties(): Boolean {
    var functionCount = 0
    return declarations.all {
        it !is KtProperty && (it !is KtNamedFunction || functionCount++ == 0)
    } && functionCount > 0
}
