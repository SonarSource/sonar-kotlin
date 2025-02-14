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
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.psiUtil.findDescendantOfType
import org.jetbrains.kotlin.resolve.BindingContext
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

  @Test
  fun testNonEmptyBindingContext() {
    val kotlinCoreEnvironment = kotlinCoreEnvironment(
      compilerConfiguration(emptyList(), LanguageVersion.KOTLIN_1_4, JvmTarget.JVM_1_8),
      disposable
    )

    assertThat(analyzeAndGetBindingContext(kotlinCoreEnvironment, emptyList()))
      .isNotEqualTo(BindingContext.EMPTY)
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

  /**
   * @see k2
   */
  @Test
  fun k1() {
    val environment = Environment(
      disposable,
      listOf(),
      LanguageVersion.LATEST_STABLE,
      JvmTarget.JVM_1_8,
      useK2 = false,
    )
    val ktFile = environment.ktPsiFactory.createFile("/fake.kt", content)
    analyzeAndGetBindingContext(environment.env, listOf(ktFile))
    kaSession(ktFile) {
      withKaSession {
        assertThat(ktFile.findDescendantOfType<KtIsExpression>()!!.expressionType.toString())
          .isEqualTo("kotlin/Boolean")
        assertThat(ktFile.findDescendantOfType<KtDotQualifiedExpression>()!!.expressionType.toString())
          .isEqualTo("kotlin/Unit")
      }
    }
  }

  /**
   * @see k1
   */
  @Test
  fun k2() {
    val analysisSession = createK2AnalysisSession(
      disposable,
      compilerConfiguration(
        listOf(),
        LanguageVersion.LATEST_STABLE,
        JvmTarget.JVM_1_8,
      ),
      listOf(KotlinVirtualFile(KotlinFileSystem(), File("/fake.kt"), content)),
    )
    val ktFile = KtPsiFactory(analysisSession.project).createFile(content)
//    val ktFile: KtFile = analysisSession.modulesWithFiles.entries.first().value[0] as KtFile
    kaSession(ktFile) {
      withKaSession {
        assertThat(ktFile.findDescendantOfType<KtIsExpression>()!!.expressionType.toString())
          .isEqualTo("kotlin/Boolean")
        assertThat(ktFile.findDescendantOfType<KtDotQualifiedExpression>()!!.expressionType.toString())
          .isEqualTo("kotlin/Int")
      }
    }
  }

}
