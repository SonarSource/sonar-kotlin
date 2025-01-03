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
package org.sonarsource.kotlin.externalreport

import org.assertj.core.api.Assertions
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteExisting
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText

fun getActual(helperPath: String? = null, mainMethod: (Array<String?>) -> Unit): String {
    val tmpDir = createTempDirectory()
    Assertions.assertThat(tmpDir.listDirectoryEntries()).isEmpty()
    val tmpFile = tmpDir.resolve("rules-test.json")
    tmpFile.createFile()

    mainMethod(arrayOf(tmpFile.toString(), helperPath))

    val actual = tmpFile.readText()

    tmpFile.deleteExisting()
    tmpDir.deleteExisting()

    return actual
}
