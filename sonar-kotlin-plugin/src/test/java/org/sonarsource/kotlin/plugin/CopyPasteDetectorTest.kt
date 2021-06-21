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
import org.assertj.core.api.ObjectAssert
import org.junit.Rule
import org.junit.jupiter.api.Test
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport
import org.junit.rules.TemporaryFolder
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonar.api.batch.sensor.cpd.internal.TokensLine
import org.sonar.api.batch.sensor.internal.SensorContextTester
import org.sonarsource.kotlin.converter.Environment
import org.sonarsource.kotlin.converter.KotlinTree
import org.sonarsource.slang.plugin.InputFileContext

@EnableRuleMigrationSupport
class CopyPasteDetectorTest {

    var tmpFolder = TemporaryFolder()
        @Rule get

    @Test
    fun test() {
        val tmpFile = tmpFolder.newFile("dummy.kt")
        val content = """
        /*
         * some licence header for example
         */
        package foo.bar
        
        import java.nio.file.Path
        
        class Bar {
            fun foo() {
                println("something")
            }
        }
        """.trimIndent()

        val sensorContext: SensorContextTester = SensorContextTester.create(tmpFolder.root)
        val inputFile = TestInputFileBuilder("moduleKey", tmpFile.name)
            .setContents(content)
            .build()

        val root = KotlinTree.of(content, Environment(emptyList()))
        val ctx = InputFileContext(sensorContext, inputFile)
        CopyPasteDetector().scan(ctx, root)

        val cpdTokenLines = sensorContext.cpdTokens(inputFile.key())!!

        Assertions.assertThat(cpdTokenLines).hasSize(5)

        assertThat(cpdTokenLines[0])
            .hasValue("classBar{")
            .hasStartLine(8)
            .hasStartUnit(1)
            .hasEndUnit(3)

        assertThat(cpdTokenLines[1])
            .hasValue("funfoo(){")
            .hasStartLine(9)
            .hasStartUnit(4)
            .hasEndUnit(8)

        assertThat(cpdTokenLines[2])
            .hasValue("""println("LITERAL")""")
            .hasStartLine(10)
            .hasStartUnit(9)
            .hasEndUnit(14)

        assertThat(cpdTokenLines[3])
            .hasValue("}")
            .hasStartLine(11)
            .hasStartUnit(15)
            .hasEndUnit(15)

        assertThat(cpdTokenLines[4])
            .hasValue("}")
            .hasStartLine(12)
            .hasStartUnit(16)
            .hasEndUnit(16)
    }
}

private class TokensLineAssert(actual: TokensLine) : ObjectAssert<TokensLine>(actual) {
    fun hasValue(expected: String) = also {
        Assertions.assertThat(actual.value).isEqualTo(expected)
    }

    fun hasStartLine(expected: Int) = also {
        Assertions.assertThat(actual.startLine).isEqualTo(expected)
    }

    fun hasStartUnit(expected: Int) = also {
        Assertions.assertThat(actual.startUnit).isEqualTo(expected)
    }

    fun hasEndUnit(expected: Int) = also {
        Assertions.assertThat(actual.endUnit).isEqualTo(expected)
    }
}

private fun assertThat(actual: TokensLine): TokensLineAssert {
    return TokensLineAssert(actual)
}
