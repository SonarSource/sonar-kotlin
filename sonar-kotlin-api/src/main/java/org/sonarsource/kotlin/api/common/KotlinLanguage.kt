/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2024 SonarSource SA
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
package org.sonarsource.kotlin.api.common

import org.sonar.api.config.Configuration
import org.sonar.api.resources.AbstractLanguage

class KotlinLanguage(
    private val configuration: Configuration,
) : AbstractLanguage(KOTLIN_LANGUAGE_KEY, KOTLIN_LANGUAGE_NAME) {
    override fun getFileSuffixes(): Array<String> =
        (configuration.getStringArray(KOTLIN_FILE_SUFFIXES_KEY)).let {
            if (it.isNullOrEmpty()) KOTLIN_FILE_SUFFIXES_DEFAULT_VALUE.split(",").toTypedArray()
            else it
        }
}
