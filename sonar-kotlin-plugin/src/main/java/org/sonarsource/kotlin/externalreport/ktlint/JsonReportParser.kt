/*
 * SonarSource SLang
 * Copyright (C) 2018-2021 SonarSource SA
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

import com.eclipsesource.json.Json
import com.eclipsesource.json.JsonArray
import com.eclipsesource.json.JsonValue
import java.nio.file.Path
import kotlin.io.path.reader

internal class JsonReportParser(private val reportFile: Path) {

    var report: Map<String, List<Finding>> = emptyMap()
        private set

    var parsingExceptions: List<InvalidReportFormatException> = emptyList()
        private set

    @Throws(InvalidReportFormatException::class)
    fun parse() {
        val jsonInput = try {
            Json.parse(reportFile.reader())
        } catch (e: Exception) {
            throw InvalidReportFormatException("JSON parsing failed: ${e.javaClass.simpleName} (${e.message})")
        }

        val parsingExceptions = mutableListOf<InvalidReportFormatException>()
        if (!jsonInput.isArray) {
            throw InvalidReportFormatException("Could not parse list of files with reported errors. Expected JSON array.")
        }

        report = jsonInput.asArray().mapIndexedNotNull { i, fileWithErrors ->
            val (findingsPerFile, exception) = parseReportEntry(i, fileWithErrors)
            exception?.let { parsingExceptions.add(it) }
            findingsPerFile?.let { it.file to it.findings }
        }.toMap()

        this.parsingExceptions = parsingExceptions
    }

    private fun parseReportEntry(index: Int, fileWithErrors: JsonValue): Pair<FindingsPerFile?, InvalidReportFormatException?> {
        if (!fileWithErrors.isObject) {
            return null to InvalidReportFormatException("Could not parse entry $index. Expected JSON object.")
        }

        val fileWithErrorsObj = fileWithErrors.asObject()
        val fileName = fileWithErrorsObj["file"].let {
            if (it?.isString == true) it.asString()
            else return null to InvalidReportFormatException("Invalid entry for file name $index.")
        }

        val (linterErrors, errorParsingExceptions) = fileWithErrorsObj["errors"].let {
            if (it?.isArray == true) extractErrors(fileName, it.asArray())
            else return null to InvalidReportFormatException("Could not parse valid list of errors for entry $index (file $fileName)")
        }

        val result = FindingsPerFile(fileName, linterErrors)

        return if (errorParsingExceptions.isNotEmpty()) {
            errorParsingExceptions.forEach { LOG.debug(it.message) }
            result to InvalidReportFormatException("Not all ktlint errors were parsed correctly for file '$fileName'.")
        } else {
            result to null
        }
    }

    private fun extractErrors(currentFile: String, listOfErrors: JsonArray): Pair<List<Finding>, List<InvalidReportFormatException>> {
        val parsingErrors = mutableListOf<InvalidReportFormatException>()
        return listOfErrors.mapIndexedNotNull { i, jsonError ->
            try {
                val error = jsonError.asObject()

                val line = error["line"].asInt()
                val column = error["column"].asInt()
                val message = error["message"].asString()
                val rule = error["rule"].asString()

                Finding(line, column, message, rule)
            } catch (e: Exception) {
                parsingErrors.add(InvalidReportFormatException("Could not parse error $i of the entry of file '$currentFile'."))
                null
            }
        } to parsingErrors
    }
}

class InvalidReportFormatException(msg: String) : RuntimeException(msg) {
    override val message: String
        get() = super.message ?: "Unexpected input encountered during parsing."
}

private data class FindingsPerFile(val file: String, val findings: List<Finding>)
data class Finding(val line: Int, val column: Int, val message: String, val rule: String)

