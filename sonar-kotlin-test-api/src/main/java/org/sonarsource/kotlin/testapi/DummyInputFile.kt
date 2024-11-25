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
package org.sonarsource.kotlin.testapi

import org.jetbrains.kotlin.idea.KotlinLanguage
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.TextPointer
import org.sonar.api.batch.fs.TextRange
import org.sonar.api.batch.fs.internal.DefaultTextPointer
import org.sonar.api.batch.fs.internal.DefaultTextRange
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

class DummyInputFile(val path: Path? = null) : InputFile {

    var status: InputFile.Status = InputFile.Status.CHANGED

    val content by lazy { path?.readText() ?: "" }

    val lines = content.split('\n')

    override fun key() = filename()

    override fun isFile() = path?.isRegularFile() ?: true

    override fun relativePath() = path().toString()

    override fun absolutePath() = path().absolute().toString()

    override fun file() = path().toFile()

    override fun path() = path ?: Path.of("./dummyFile.kt")

    override fun uri() = path().toUri()

    override fun filename() = path().fileName.toString()

    override fun language() = KotlinLanguage.NAME

    override fun type() = InputFile.Type.MAIN

    override fun inputStream() = content.byteInputStream()

    override fun contents() = content

    override fun status() = status

    override fun lines() = lines.size

    override fun isEmpty(): Boolean = content.isEmpty()

    override fun newPointer(line: Int, lineOffset: Int) = DefaultTextPointer(line, lineOffset)

    override fun newRange(start: TextPointer, end: TextPointer) = DefaultTextRange(start, end)

    override fun newRange(startLine: Int, startLineOffset: Int, endLine: Int, endLineOffset: Int) =
        newRange(newPointer(startLine, startLineOffset), newPointer(endLine, endLineOffset))

    override fun selectLine(line: Int): TextRange {
        require(line <= lines.size) { "Line not in file" }
        return newRange(line, 0, line, lines[line - 1].length)
    }

    override fun charset() = Charsets.UTF_8

    override fun md5Hash(): String {
        throw UnsupportedOperationException()
    }
}
