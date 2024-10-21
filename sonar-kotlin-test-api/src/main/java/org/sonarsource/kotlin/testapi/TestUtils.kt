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
package org.sonarsource.kotlin.testapi

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.diagnostics.MutableDiagnosticsWithSuppression
import org.sonar.api.batch.fs.InputFile
// TODO: testapi should not depend on frontend module.
import org.sonarsource.kotlin.api.frontend.Environment
import org.sonarsource.kotlin.api.frontend.KotlinSyntaxStructure
import org.sonarsource.kotlin.api.frontend.KotlinTree
import org.sonarsource.kotlin.api.frontend.RegexCache
import org.sonarsource.kotlin.api.frontend.bindingContext

fun kotlinTreeOf(content: String, environment: Environment, inputFile: InputFile, doResolve: Boolean = true, providedDiagnostics: List<Diagnostic>? = null): KotlinTree {
    val (ktFile, document) = KotlinSyntaxStructure.of(content, environment, inputFile)

    val bindingContext =
    if (environment.session != null) BindingContext.EMPTY else
    if (doResolve) bindingContext(
        environment.env,
        environment.classpath,
        listOf(ktFile),
    ) else BindingContext.EMPTY

    val diagnostics = bindingContext.diagnostics;
    val diagnosticsList = diagnostics.noSuppression().toList()
    if (diagnostics is MutableDiagnosticsWithSuppression) {
        diagnostics.clear()
    }

    return KotlinTree(ktFile, document, bindingContext, providedDiagnostics ?: diagnosticsList, RegexCache(), doResolve)
}
