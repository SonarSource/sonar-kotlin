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
package org.sonarsource.kotlin.testapi

import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.resolve.BindingContext
import org.sonar.api.batch.fs.InputFile
// TODO: testapi should not depend on frontend module.
import org.sonarsource.kotlin.api.frontend.Environment
import org.sonarsource.kotlin.api.frontend.KotlinFileSystem
import org.sonarsource.kotlin.api.frontend.KotlinSyntaxStructure
import org.sonarsource.kotlin.api.frontend.KotlinTree
import org.sonarsource.kotlin.api.frontend.KotlinVirtualFile
import org.sonarsource.kotlin.api.frontend.RegexCache
import org.sonarsource.kotlin.api.frontend.analyzeAndGetBindingContext
import org.sonarsource.kotlin.api.frontend.createK2AnalysisSession
import java.io.File

fun kotlinTreeOf(content: String, environment: Environment, inputFile: InputFile, doResolve: Boolean = true, providedDiagnostics: List<Diagnostic>? = null): KotlinTree {
    if (environment.useK2) {
        val virtualFile = KotlinVirtualFile(
            KotlinFileSystem(),
            File(inputFile.uri().path),
            content,
        )
        environment.k2session = createK2AnalysisSession(
            environment.disposable,
            environment.configuration,
            listOf(virtualFile),
        )
    }

    val (ktFile, document) = KotlinSyntaxStructure.of(content, environment, inputFile)

    val bindingContext = if (!environment.useK2 && doResolve) analyzeAndGetBindingContext(
        environment.env,
        listOf(ktFile),
    ) else BindingContext.EMPTY
    return KotlinTree(ktFile, document, bindingContext, providedDiagnostics ?: bindingContext.diagnostics.noSuppression().toList(), RegexCache(), doResolve)
}
