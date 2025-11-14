/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource Sàrl
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource Sàrl.
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

import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.analysis.api.analyze
import org.jetbrains.kotlin.analysis.api.components.KaDiagnosticCheckerFilter
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonarsource.kotlin.api.frontend.Environment
import org.sonarsource.kotlin.testapi.kotlinTreeOf
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import kotlin.io.path.name

internal class UselessAssignmentsCheckTest : CheckTest(UselessAssignmentsCheck()) {
    private val disposable = Disposer.newDisposable()

    @AfterEach
    fun dispose() {
        Disposer.dispose(disposable)
    }

    @Test
    fun prefix_operator() {
        val file = Path.of("/fake.kt")
        val content = """
                fun main() {
                  var i = 0
                  use(++i)
                }
                fun use(i: Int) {}
            """.trimIndent()
        val ktFile = kotlinTreeOf(
            content,
            Environment(disposable, emptyList(), LanguageVersion.LATEST_STABLE),
            TestInputFileBuilder("moduleKey", file.name)
                .setModuleBaseDir(file.parent)
                .setCharset(StandardCharsets.UTF_8)
                .initMetadata(content).build(),
        ).psiFile
        analyze(ktFile) {
            // Failure of the following assertion during Kotlin compiler version upgrade will indicate fix of
            // https://youtrack.jetbrains.com/issue/KT-75695/Bogus-Assigned-value-is-never-read-warning-for-prefix-operator
            assertTrue(
                ktFile.collectDiagnostics(KaDiagnosticCheckerFilter.EXTENDED_AND_COMMON_CHECKERS)
                    .any { it.factoryName == FirErrors.ASSIGNED_VALUE_IS_NEVER_READ.name }
            )
        }
    }
}
