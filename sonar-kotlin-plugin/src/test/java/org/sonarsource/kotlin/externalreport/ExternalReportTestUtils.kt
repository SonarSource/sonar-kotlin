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
package org.sonarsource.kotlin.externalreport

import org.assertj.core.api.Assertions.assertThat
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonar.api.batch.sensor.internal.SensorContextTester
import org.sonar.api.utils.log.LoggerLevel
import org.sonar.api.utils.log.ThreadLocalLogTester
import org.sonarsource.kotlin.plugin.KotlinPlugin
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

object ExternalReportTestUtils {

    @Throws(IOException::class)
    fun createContext(projectDir: Path): SensorContextTester {
        val context = SensorContextTester.create(projectDir)
        Files.list(projectDir)
            .filter { file: Path -> !Files.isDirectory(file) }
            .forEach { file: Path -> addFileToContext(context, projectDir, file) }
        return context
    }

    fun assertNoErrorWarnDebugLogs(logTester: ThreadLocalLogTester) {
        assertThat(logTester.logs(LoggerLevel.ERROR)).isEmpty()
        assertThat(logTester.logs(LoggerLevel.WARN)).isEmpty()
        assertThat(logTester.logs(LoggerLevel.DEBUG)).isEmpty()
    }

    fun onlyOneLogElement(elements: List<String>): String {
        assertThat(elements).hasSize(1)
        return elements[0]
    }

    private fun addFileToContext(context: SensorContextTester, projectDir: Path, file: Path) {
        try {
            val projectId = projectDir.fileName.toString() + "-project"
            context.fileSystem().add(TestInputFileBuilder.create(projectId, projectDir.toFile(), file.toFile())
                .setCharset(StandardCharsets.UTF_8)
                .setLanguage(language(file))
                .setContents(String(Files.readAllBytes(file), StandardCharsets.UTF_8))
                .setType(InputFile.Type.MAIN)
                .build())
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }
    }

    private fun language(file: Path): String {
        val path = file.toString()
        return if (path.endsWith(".kt")) KotlinPlugin.KOTLIN_LANGUAGE_KEY
        else path.substring(path.lastIndexOf('.') + 1)
    }
}
