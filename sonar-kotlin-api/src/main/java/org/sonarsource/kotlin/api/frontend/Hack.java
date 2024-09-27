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
package org.sonarsource.kotlin.api.frontend;

import org.jetbrains.kotlin.analysis.api.standalone.base.projectStructure.KotlinStaticProjectStructureProvider;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreProjectEnvironment;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.psi.KtFile;

import java.util.List;

final class Hack {

    static KotlinStaticProjectStructureProvider buildKtModuleProviderByCompilerConfiguration(
            KotlinCoreProjectEnvironment kotlinCoreProjectEnvironment,
            CompilerConfiguration compilerConfig,
            List<KtFile> ktFiles
    ) {
        return org.jetbrains.kotlin.analysis.project.structure.impl.KaModuleUtilsKt.buildKtModuleProviderByCompilerConfiguration(
                kotlinCoreProjectEnvironment, compilerConfig, ktFiles
        );
    }

    private Hack() {
    }

}
