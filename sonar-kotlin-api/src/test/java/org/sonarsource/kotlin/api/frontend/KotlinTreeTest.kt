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

import com.intellij.openapi.util.Disposer
import java.nio.file.Files
import java.nio.file.Path
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonarsource.kotlin.testapi.kotlinTreeOf
import java.nio.charset.StandardCharsets

class KotlinTreeTest {

  private val disposable = Disposer.newDisposable()

  @AfterEach
  fun dispose() {
    Disposer.dispose(disposable)
  }

  @Test
  fun testCreateKotlinTree() {
    val environment = Environment(disposable, listOf("../kotlin-checks-test-sources/build/classes/kotlin/main"), LanguageVersion.LATEST_STABLE)
    val path = Path.of("../kotlin-checks-test-sources/src/main/kotlin/sample/functions.kt")
    val content = String(Files.readAllBytes(path))
    val inputFile = TestInputFileBuilder("moduleKey",  "src/org/foo/kotlin.kt")
      .setCharset(StandardCharsets.UTF_8)
      .initMetadata(content)
      .build()

    val tree = kotlinTreeOf(content, environment, inputFile)
    assertThat(tree.psiFile.children).hasSize(9)

    assertThat(tree.bindingContext.getSliceContents(BindingContext.RESOLVED_CALL)).hasSize(22)

    val ktCallExpression = tree.psiFile.children[3].children[2].children[1].children[1].children[0] as KtElement
    val call = tree.bindingContext.get(BindingContext.CALL, ktCallExpression)
    val resolvedCall = tree.bindingContext.get(BindingContext.RESOLVED_CALL, call)
    assertThat(resolvedCall).isNotNull
  }
}
