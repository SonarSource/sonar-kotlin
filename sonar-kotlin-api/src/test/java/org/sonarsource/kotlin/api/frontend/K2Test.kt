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
package org.sonarsource.kotlin.api.frontend

import com.intellij.openapi.util.Disposer
import com.intellij.psi.util.descendantsOfType
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.junit.jupiter.api.Test
import java.io.File

class K2Test {

    @Test
    fun test() {
        val disposable = Disposer.newDisposable()
        val compilerConfiguration = compilerConfiguration(
            System.getProperty("java.class.path").split(File.pathSeparatorChar),
            LanguageVersion.LATEST_STABLE,
            JvmTarget.JVM_1_8,
            0
        )
        val analysisSession = createK2AnalysisSession(
            disposable,
            compilerConfiguration,
            listOf(
                KotlinVirtualFile(
                    KotlinFileSystem(), File("/fake.kt"), """
                    fun main() {
                      return 0
                    }
                    """.trimIndent()
                )
            ),
        )
        val ktFile: KtFile = analysisSession.modulesWithFiles.entries.first().value[0] as KtFile
        org.jetbrains.kotlin.analysis.api.analyze(ktFile) {
            ktFile.descendantsOfType<KtExpression>().forEach {
                println(it.expressionType)
            }
        }
        Disposer.dispose(disposable)
    }

}
