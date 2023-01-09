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
package org.sonarsource.kotlin.converter

import java.nio.file.Files
import java.nio.file.Path
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.junit.jupiter.api.Test
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonarsource.kotlin.utils.kotlinTreeOf
import java.nio.charset.StandardCharsets

class KotlinTreeTest {

  @Test
  fun testCreateKotlinTree() {
    val environment = Environment(listOf("../kotlin-checks-test-sources/build/classes/kotlin/main"), LanguageVersion.LATEST_STABLE)
    val path = Path.of("../kotlin-checks-test-sources/src/main/kotlin/sample/functions.kt")
    val content = String(Files.readAllBytes(path))
    val inputFile = TestInputFileBuilder("moduleKey",  "src/org/foo/kotlin.kt")
      .setCharset(StandardCharsets.UTF_8)
      .initMetadata(content)
      .build()

    val tree = kotlinTreeOf(content, environment, inputFile)
    assertThat(tree.psiFile.children).hasSize(9)

    assertThat(tree.bindingContext.getSliceContents(BindingContext.RESOLVED_CALL)).hasSize(19)

    val ktCallExpression = tree.psiFile.children[3].children[2].children[1].children[1].children[0] as KtElement
    val call = tree.bindingContext.get(BindingContext.CALL, ktCallExpression)
    val resolvedCall = tree.bindingContext.get(BindingContext.RESOLVED_CALL, call)
    assertThat(resolvedCall).isNotNull
  }
}
