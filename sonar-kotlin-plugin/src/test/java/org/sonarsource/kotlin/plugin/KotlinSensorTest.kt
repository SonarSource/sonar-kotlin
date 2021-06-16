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
package org.sonarsource.kotlin.plugin

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.sonar.api.batch.rule.CheckFactory
import org.sonar.api.batch.sensor.highlighting.TypeOfText
import org.sonar.api.config.internal.MapSettings
import org.sonar.api.issue.NoSonarFilter
import org.sonar.api.measures.CoreMetrics
import org.sonarsource.slang.testing.AbstractSensorTest
import org.sonarsource.slang.testing.TextRangeAssert

internal class KotlinSensorTest : AbstractSensorTest() {

    @Test
    fun test_one_rule() {
        val inputFile = createInputFile("file1.kt", """
     fun main(args: Array<String>) {
     print (1 == 1);}
     """.trimIndent())
        context.fileSystem().add(inputFile)
        val checkFactory = checkFactory("S1764")
        sensor(checkFactory).execute(context)
        val issues = context.allIssues()
        Assertions.assertThat(issues).hasSize(1)
        val issue = issues.iterator().next()
        Assertions.assertThat(issue.ruleKey().rule()).isEqualTo("S1764")
        val location = issue.primaryLocation()
        Assertions.assertThat(location.inputComponent()).isEqualTo(inputFile)
        Assertions.assertThat(location.message())
            .isEqualTo("Correct one of the identical sub-expressions on both sides this operator")
        TextRangeAssert.assertTextRange(location.textRange()).hasRange(2, 12, 2, 13)
    }

    @Test
    fun test_commented_code() {
        val inputFile = createInputFile("file1.kt", """
     fun main(args: Array<String>) {
     //fun foo () { if (true) {print("string literal");}}
     print (1 == 1);
     print(b);
     //a b c ...
     foo();
     // Coefficients of polynomial
     val b = DoubleArray(n) // linear
     val c = DoubleArray(n + 1) // quadratic
     val d = DoubleArray(n) // cubic
     }
     """.trimIndent())
        context.fileSystem().add(inputFile)
        val checkFactory = checkFactory("S125")
        sensor(checkFactory).execute(context)
        val issues = context.allIssues()
        Assertions.assertThat(issues).hasSize(1)
        val issue = issues.iterator().next()
        Assertions.assertThat(issue.ruleKey().rule()).isEqualTo("S125")
        val location = issue.primaryLocation()
        Assertions.assertThat(location.inputComponent()).isEqualTo(inputFile)
        Assertions.assertThat(location.message()).isEqualTo("Remove this commented out code.")
    }

    @Test
    fun simple_file() {
        val inputFile = createInputFile("file1.kt", """
     fun main(args: Array<String>) {
     print (1 == 1); print("abc"); }
     data class A(val a: Int)
     """.trimIndent())
        context.fileSystem().add(inputFile)
        sensor(checkFactory()).execute(context)
        Assertions.assertThat(context.highlightingTypeAt(inputFile.key(), 1, 0)).containsExactly(TypeOfText.KEYWORD)
        Assertions.assertThat(context.highlightingTypeAt(inputFile.key(), 1, 3)).isEmpty()
        Assertions.assertThat(context.measure(inputFile.key(), CoreMetrics.NCLOC).value()).isEqualTo(3)
        Assertions.assertThat(context.measure(inputFile.key(), CoreMetrics.COMMENT_LINES).value()).isZero
        Assertions.assertThat(context.measure(inputFile.key(), CoreMetrics.FUNCTIONS).value()).isEqualTo(1)
        Assertions.assertThat(context.measure(inputFile.key(), CoreMetrics.CLASSES).value()).isEqualTo(1)
        Assertions.assertThat(context.cpdTokens(inputFile.key())!![1].value)
            .isEqualTo("print(1==1);print(\"LITERAL\");}")
        Assertions.assertThat(context.measure(inputFile.key(), CoreMetrics.COMPLEXITY).value()).isEqualTo(1)
        Assertions.assertThat(context.measure(inputFile.key(), CoreMetrics.STATEMENTS).value()).isEqualTo(2)

        // FIXME
        //assertThat(logTester.logs()).contains("1 source files to be analyzed");
    }

    @Test
    fun test_issue_suppression() {
        val inputFile = createInputFile("file1.kt", """
     @SuppressWarnings("kotlin:S1764")
     fun main() {
     print (1 == 1);}
     @SuppressWarnings(value=["kotlin:S1764"])
     fun main2() {
     print (1 == 1);}
     """.trimIndent())
        context.fileSystem().add(inputFile)
        val checkFactory = checkFactory("S1764")
        sensor(checkFactory).execute(context)
        val issues = context.allIssues()
        Assertions.assertThat(issues).isEmpty()
    }

    @Test
    fun test_issue_not_suppressed() {
        val inputFile = createInputFile("file1.kt", """
     @SuppressWarnings("S1764")
     fun main() {
     print (1 == 1);}
     @SuppressWarnings(value=["scala:S1764"])
     fun main2() {
     print (1 == 1);}
     """.trimIndent())
        context.fileSystem().add(inputFile)
        val checkFactory = checkFactory("S1764")
        sensor(checkFactory).execute(context)
        val issues = context.allIssues()
        Assertions.assertThat(issues).hasSize(2)
    }

    @Test
    fun test_fail_parsing() {
        val inputFile = createInputFile("file1.kt", "" +
            "enum class A { <!REDECLARATION!>FOO<!>,<!REDECLARATION!>FOO<!> }")
        context.fileSystem().add(inputFile)
        val checkFactory = checkFactory("S1764")
        sensor(checkFactory).execute(context)
        val analysisErrors = context.allAnalysisErrors()
        Assertions.assertThat(analysisErrors).hasSize(1)
        val analysisError = analysisErrors.iterator().next()
        Assertions.assertThat(analysisError.inputFile()).isEqualTo(inputFile)
        Assertions.assertThat(analysisError.message()).isEqualTo("Unable to parse file: file1.kt")
        val textPointer = analysisError.location()
        Assertions.assertThat(textPointer).isNotNull
        Assertions.assertThat(textPointer!!.line()).isEqualTo(1)
        Assertions.assertThat(textPointer.lineOffset()).isEqualTo(14)
        Assertions.assertThat(logTester.logs())
            .contains(String.format("Unable to parse file: %s. Parse error at position 1:14", inputFile.uri()))
    }

    @Test
    fun test_with_classpath() {
        val settings = MapSettings()
        settings.setProperty(KotlinPlugin.SONAR_JAVA_BINARIES, "classes/")
        context.setSettings(settings)
        val inputFile = createInputFile("file1.kt", "class A { fun f() = TODO() }")
        context.fileSystem().add(inputFile)
        val checkFactory = checkFactory("S1764")
        sensor(checkFactory).execute(context)
        val analysisErrors = context.allAnalysisErrors()
        Assertions.assertThat(analysisErrors).isEmpty()
    }

    @Test
    fun test_with_blank_classpath() {
        val settings = MapSettings()
        settings.setProperty(KotlinPlugin.SONAR_JAVA_BINARIES, " ")
        context.setSettings(settings)
        val inputFile = createInputFile("file1.kt", "class A { fun f() = TODO() }")
        context.fileSystem().add(inputFile)
        val checkFactory = checkFactory("S1764")
        sensor(checkFactory).execute(context)
        val analysisErrors = context.allAnalysisErrors()
        Assertions.assertThat(analysisErrors).isEmpty()
    }

    override fun repositoryKey(): String {
        return KotlinPlugin.KOTLIN_REPOSITORY_KEY
    }

    override fun language(): KotlinLanguage {
        return KotlinLanguage(MapSettings().asConfig())
    }

    private fun sensor(checkFactory: CheckFactory): KotlinSensor {
        return KotlinSensor(checkFactory, fileLinesContextFactory, NoSonarFilter(), language())
    }
}
