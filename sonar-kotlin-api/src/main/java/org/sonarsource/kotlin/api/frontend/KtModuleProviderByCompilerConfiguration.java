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
package org.sonarsource.kotlin.api.frontend;

import kotlin.jvm.functions.Function1;
import org.jetbrains.kotlin.analysis.api.standalone.StandaloneAnalysisAPISessionBuilder;
import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.KotlinStaticProjectStructureProvider;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.psi.KtFile;

import java.util.List;

/**
 * @deprecated use {@link StandaloneAnalysisAPISessionBuilder#buildKtModuleProvider(Function1)} instead
 */
@Deprecated(forRemoval = true)
final class KtModuleProviderByCompilerConfiguration {

  @SuppressWarnings("KotlinInternalInJava")
  static KotlinStaticProjectStructureProvider build(
      KotlinCoreProjectEnvironment kotlinCoreProjectEnvironment,
      CompilerConfiguration compilerConfig,
      List<KtFile> ktFiles
  ) {
    return org.jetbrains.kotlin.analysis.project.structure.impl
        .KaModuleUtilsKt.buildKtModuleProviderByCompilerConfiguration(
            kotlinCoreProjectEnvironment,
            compilerConfig,
            ktFiles
        );
  }

  private KtModuleProviderByCompilerConfiguration() {
  }

}
