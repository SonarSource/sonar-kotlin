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

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISession
import org.jetbrains.kotlin.analysis.api.standalone.buildStandaloneAnalysisAPISession
import org.jetbrains.kotlin.cli.common.CliModuleVisibilityManagerImpl
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.load.kotlin.ModuleVisibilityManager

fun createAnalysisSession(
    parentDisposable: Disposable,
    compilerConfiguration: CompilerConfiguration,
): StandaloneAnalysisAPISession {
    return buildStandaloneAnalysisAPISession(
        projectDisposable = parentDisposable,
    ) {
        // https://github.com/JetBrains/kotlin/blob/a9ff22693479cabd201909a06e6764c00eddbf7b/analysis/analysis-api-fe10/tests/org/jetbrains/kotlin/analysis/api/fe10/test/configurator/AnalysisApiFe10TestServiceRegistrar.kt#L49
        registerProjectService(ModuleVisibilityManager::class.java, CliModuleVisibilityManagerImpl(enabled = true))

        compilerConfiguration.addKotlinSourceRoot("../kotlin-checks-test-sources/src/main/kotlin")
        buildKtModuleProviderByCompilerConfiguration(compilerConfiguration)
    }
}
