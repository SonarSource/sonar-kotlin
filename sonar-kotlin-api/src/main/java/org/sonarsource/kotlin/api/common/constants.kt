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
package org.sonarsource.kotlin.api.common

import org.jetbrains.kotlin.config.LanguageVersion

// TODO: Maybe rework; all of these constants do not really belong her but should be responsibility of "plugin".
const val RULE_REPOSITORY_LANGUAGE = "kotlin"
const val KOTLIN_REPOSITORY_KEY = "kotlin"
const val KOTLIN_LANGUAGE_KEY = "kotlin"
const val KOTLIN_LANGUAGE_NAME = "Kotlin"

const val KOTLIN_FILE_SUFFIXES_KEY = "sonar.kotlin.file.suffixes"
const val KOTLIN_FILE_SUFFIXES_DEFAULT_VALUE = ".kt,.kts"
const val KOTLIN_LANGUAGE_VERSION = "sonar.kotlin.source.version"

val DEFAULT_KOTLIN_LANGUAGE_VERSION = LanguageVersion.LATEST_STABLE
const val FAIL_FAST_PROPERTY_NAME = "sonar.internal.analysis.failFast"

// https://jira.sonarsource.com/browse/SONARKT-242
@Deprecated("was used, but not anymore, preserved for future")
const val COMPILER_THREAD_COUNT_PROPERTY = "sonar.kotlin.threads"

const val SONAR_ANDROID_DETECTED = "sonar.android.detected"
const val SONAR_JAVA_BINARIES = "sonar.java.binaries"
const val SONAR_JAVA_LIBRARIES = "sonar.java.libraries"
