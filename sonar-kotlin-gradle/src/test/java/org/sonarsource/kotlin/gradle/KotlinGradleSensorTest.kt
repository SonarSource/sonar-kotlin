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
package org.sonarsource.kotlin.gradle

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.sonar.api.batch.ScannerSide
import org.sonar.api.batch.rule.CheckFactory
import org.sonarsource.kotlin.api.frontend.RegexCache
import org.sonarsource.kotlin.api.frontend.bindingContext
import org.sonarsource.kotlin.api.logging.debug
import org.sonarsource.kotlin.api.visiting.KotlinFileVisitor
import org.sonarsource.kotlin.testapi.AbstractSensorTest
import org.sonarsource.kotlin.visiting.KtChecksVisitor
import org.sonarsource.performance.measure.PerformanceMeasure
import java.io.File

import java.util.concurrent.TimeUnit
import kotlin.io.path.createFile

private val LOG = LoggerFactory.getLogger(KotlinGradleSensor::class.java)
private val EMPTY_FILE_CONTENT_PATTERN = Regex("""\s*+""")

@ScannerSide
class KotlinGradleSensorTest: AbstractSensorTest() {

    @Test
    fun test_one_rule() {
        val inputFile = createInputFile(
            "settings.gradle.kts", """
     fun main(args: Array<String>) {
     print (1 == 1);}
     """.trimIndent())

        baseDir.resolve("settings.gradle.kts").createFile()

        context.fileSystem().add(inputFile)
        val checkFactory = checkFactory("S1764")
        sensor(checkFactory).execute(context)
        val issues = context.allIssues()
    }
    private fun sensor(checkFactory: CheckFactory): KotlinGradleSensor {
        return KotlinGradleSensor(checkFactory, language())
    }

}

