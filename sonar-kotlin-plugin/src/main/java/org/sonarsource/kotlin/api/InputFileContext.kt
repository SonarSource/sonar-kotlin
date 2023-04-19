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
package org.sonarsource.kotlin.api

import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.TextPointer
import org.sonar.api.batch.fs.TextRange
import org.sonar.api.batch.sensor.SensorContext
import org.sonar.api.rule.RuleKey

interface InputFileContext {
    var filteredRules: Map<String, Set<TextRange>>
    val inputFile: InputFile
    val sensorContext: SensorContext
    val isAndroid: Boolean

    fun reportIssue(
        ruleKey: RuleKey,
        textRange: TextRange?,
        message: Message,
        secondaryLocations: List<SecondaryLocation>,
        gap: Double?,
    )

    fun reportAnalysisParseError(repositoryKey: String, inputFile: InputFile, location: TextPointer?)

    fun reportAnalysisError(message: String?, location: TextPointer?)
}
