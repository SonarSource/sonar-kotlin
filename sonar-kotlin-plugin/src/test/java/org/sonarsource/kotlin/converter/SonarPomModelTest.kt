/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.pom.PomModelAspect
import org.jetbrains.kotlin.com.intellij.pom.PomTransaction
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion
import org.junit.jupiter.api.Test

class SonarPomModelTest {
  
  @Test
  fun testSonarPomModel() {
    val kotlinCoreEnvironment = kotlinCoreEnvironment(
      compilerConfiguration(emptyList(), LanguageVersion.KOTLIN_1_4, JvmTarget.JVM_1_8),
      Disposer.newDisposable()
    )

    val sonarPomModel = SonarPomModel(kotlinCoreEnvironment.project)
    
    sonarPomModel.runTransaction(object : PomTransaction{})
    
    assertThat(sonarPomModel.getModelAspect(PomModelAspect::class.java)).isNull()
  }
}
