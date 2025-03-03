/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
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
package org.sonarsource.kotlin.api.frontend

import org.assertj.core.api.Assertions.assertThat
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.api.visiting.kaSession
import org.sonarsource.kotlin.api.visiting.withKaSession
import java.io.File

class KotlinCoreEnvironmentToolsTest {

  private val disposable = Disposer.newDisposable()

  @AfterEach
  fun dispose() {
    Disposer.dispose(disposable)
  }

  // https://kotlinlang.org/docs/whatsnew20.html#smart-cast-improvements
  private val content = """
    fun example(any: Any) {
      val isString = any is String
      if (isString) {
        any.length
      }
    }
    """.trimIndent()

  @Test
  fun k2() {
    val analysisSession = createK2AnalysisSession(
      disposable,
      compilerConfiguration(
        listOf(),
        LanguageVersion.LATEST_STABLE,
        JvmTarget.JVM_1_8,
      ),
      listOf(KotlinVirtualFile(KotlinFileSystem(), File("/fake.kt"), contentProvider = { content })),
    )
    val ktFile: KtFile = analysisSession.modulesWithFiles.entries.first().value[0] as KtFile
    kaSession(ktFile) {
      withKaSession {
        assertThat(ktFile.findDescendantOfType<KtIsExpression>()!!.expressionType.toString())
          .isEqualTo("kotlin/Boolean")
        assertThat(ktFile.findDescendantOfType<KtDotQualifiedExpression>()!!.expressionType.toString())
          .isEqualTo("kotlin/Int")
      }
    }
  }

  @Test
  fun k2_language_version_1_9() {
    val analysisSession = createK2AnalysisSession(
      disposable,
      compilerConfiguration(
        listOf(),
        LanguageVersion.KOTLIN_1_9,
        JvmTarget.JVM_1_8,
      ),
      listOf(KotlinVirtualFile(KotlinFileSystem(), File("/fake.kt"), contentProvider = { content })),
    )
    val ktFile: KtFile = analysisSession.modulesWithFiles.entries.first().value[0] as KtFile
    kaSession(ktFile) {
      withKaSession {
        assertThat(ktFile.findDescendantOfType<KtIsExpression>()!!.expressionType.toString())
          .isEqualTo("kotlin/Boolean")
        assertThat(ktFile.findDescendantOfType<KtDotQualifiedExpression>()!!.expressionType is KaErrorType)
          .isTrue()
      }
    }
  }

}
