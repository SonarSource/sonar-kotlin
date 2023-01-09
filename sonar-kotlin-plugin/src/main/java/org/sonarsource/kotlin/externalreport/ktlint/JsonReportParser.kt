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
package org.sonarsource.kotlin.externalreport.ktlint

import com.google.gson.Gson
import java.nio.file.Path
import kotlin.io.path.reader

internal class JsonReportParser(private val reportFile: Path) {

    var report: List<FindingsPerFile> = emptyList()
        private set

    private val gson = Gson()

    @Throws(InvalidReportFormatException::class)
    fun parse() {
        report = try {
            gson.fromJson(reportFile.reader(), Array<FindingsPerFile>::class.java)
        } catch (e: Exception) {
            throw InvalidReportFormatException("JSON parsing failed: ${e.javaClass.simpleName} (${e.message})")
        }.toList()
    }
}

class InvalidReportFormatException(msg: String) : RuntimeException(msg) {
    override val message: String
        get() = super.message ?: "Unexpected input encountered during parsing."
}

data class FindingsPerFile(val file: String, val errors: List<Finding>)
data class Finding(val line: Int, val column: Int, val message: String, val rule: String)

