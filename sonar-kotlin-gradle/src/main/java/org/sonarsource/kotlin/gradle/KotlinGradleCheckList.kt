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
package org.sonarsource.kotlin.gradle

import org.sonarsource.kotlin.api.checks.KotlinCheck
import org.sonarsource.kotlin.gradle.checks.CorePluginsShortcutUsageCheck
import org.sonarsource.kotlin.gradle.checks.DependencyGroupedCheck
import org.sonarsource.kotlin.gradle.checks.DependencyVersionHardcodedCheck
import org.sonarsource.kotlin.gradle.checks.MissingSettingsCheck
import org.sonarsource.kotlin.gradle.checks.AndroidReleaseBuildObfuscationCheck
import org.sonarsource.kotlin.gradle.checks.RootProjectNamePresentCheck
import org.sonarsource.kotlin.gradle.checks.TaskDefinitionsCheck
import org.sonarsource.kotlin.gradle.checks.TaskRegisterVsCreateCheck


val KOTLIN_GRADLE_CHECKS: List<Class<out KotlinCheck>> = listOf(
    AndroidReleaseBuildObfuscationCheck::class.java,
    CorePluginsShortcutUsageCheck::class.java,
    RootProjectNamePresentCheck::class.java,
    DependencyGroupedCheck::class.java,
    DependencyVersionHardcodedCheck::class.java,
    MissingSettingsCheck::class.java,
    TaskDefinitionsCheck::class.java,
    TaskRegisterVsCreateCheck::class.java,
)

