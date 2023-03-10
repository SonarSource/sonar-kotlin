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
package org.sonarsource.kotlin

import org.jetbrains.kotlin.idea.KotlinLanguage
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.TextPointer
import org.sonar.api.batch.fs.TextRange
import org.sonar.api.batch.fs.internal.DefaultTextPointer
import org.sonar.api.batch.fs.internal.DefaultTextRange
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

class DummyInputFile(val path: Path? = null) : InputFile {

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

    override fun status() = InputFile.Status.CHANGED

    override fun lines() = lines.size

    override fun isEmpty(): Boolean = content.isEmpty()

    override fun newPointer(line: Int, lineOffset: Int) = DefaultTextPointer(line, lineOffset)

    override fun newRange(start: TextPointer, end: TextPointer) = DefaultTextRange(start, end)

    override fun newRange(startLine: Int, startLineOffset: Int, endLine: Int, endLineOffset: Int) =
        newRange(newPointer(startLine, startLineOffset), newPointer(endLine, endLineOffset))

    override fun selectLine(line: Int): TextRange {
        if (line > lines.size) throw IllegalArgumentException("Line not in file")
        return newRange(line, 0, line, lines[line - 1].length)
    }

    override fun charset() = Charsets.UTF_8
}
