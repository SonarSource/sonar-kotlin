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

import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.types.SimpleType
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.api.getType
import org.sonarsource.kotlin.api.hasExactlyOneFunctionAndNoProperties
import org.sonarsource.kotlin.api.isFunctionalInterface
import org.sonarsource.kotlin.api.merge
import org.sonarsource.kotlin.plugin.KotlinFileContext

@Rule(key = "S6516")
class SamConversionCheck : AbstractCheck() {

    override fun visitObjectDeclaration(declaration: KtObjectDeclaration, context: KotlinFileContext) {
        val superTypeEntry = declaration.superTypeListEntries.singleOrNull() ?: return
        val superType = superTypeEntry.typeReference?.getType(context.bindingContext) as? SimpleType ?: return

        if (superType.isFunctionalInterface() && declaration.hasExactlyOneFunctionAndNoProperties()) {
            val textRange = context.merge(declaration.getDeclarationKeyword()!!, superTypeEntry)
            context.reportIssue(textRange, "Replace explicit functional interface implementation with lambda expression.")
        }
    }
}
