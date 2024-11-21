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
package org.sonarsource.kotlin.externalreport.common

import org.assertj.core.api.Assertions.assertThat
import org.slf4j.event.Level
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonar.api.batch.sensor.internal.SensorContextTester
import org.sonar.api.testfixtures.log.LogTesterJUnit5
import org.sonarsource.kotlin.api.common.RULE_REPOSITORY_LANGUAGE
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

    fun assertNoErrorWarnDebugLogs(logTester: LogTesterJUnit5) {
        assertThat(logTester.logs(Level.ERROR)).isEmpty()
        assertThat(logTester.logs(Level.WARN)).isEmpty()
        assertThat(logTester.logs(Level.DEBUG)).isEmpty()
    }

    fun onlyOneLogElement(elements: List<String>): String {
        assertThat(elements).hasSize(1)
        return elements[0]
    }

    private fun addFileToContext(context: SensorContextTester, projectDir: Path, file: Path) {
        try {
            val projectId = projectDir.fileName.toString() + "-project"
            context.fileSystem().add(
                TestInputFileBuilder.create(projectId, projectDir.toFile(), file.toFile())
                    .setCharset(StandardCharsets.UTF_8)
                    .setLanguage(language(file))
                    .setContents(String(Files.readAllBytes(file), StandardCharsets.UTF_8))
                    .setType(InputFile.Type.MAIN)
                    .build()
            )
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }
    }

    private fun language(file: Path): String {
        val path = file.toString()
        return if (path.endsWith(".kt")) RULE_REPOSITORY_LANGUAGE
        else path.substring(path.lastIndexOf('.') + 1)
    }
}
