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
package org.sonarsource.kotlin.converter

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.dev.AstPrinter
import org.sonarsource.slang.api.ParseException
import org.sonarsource.slang.visitors.TreePrinter
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.readText

private val environment = Environment(emptyList())

fun main() {
    fix_all_cls_files_test_automatically()
}

internal class KotlinASTTest {
    @Test
    fun all_kotlin_files() {
        for (kotlinPath in kotlinSources()) {
            val astPath = Path.of(kotlinPath.toString().replaceFirst("\\.kts?$".toRegex(), ".txt"))
            val kotlinTree = parse(kotlinPath)
            val actualAst = AstPrinter.txtPrint(kotlinTree.psiFile, kotlinTree.document)
            val expectingAst = if (astPath.toFile().exists()) astPath.readText() else ""
            Assertions.assertThat(actualAst.trim { it <= ' ' })
                .describedAs("In the file: $astPath (run KotlinASTTest.main manually)")
                .isEqualTo(expectingAst.trim { it <= ' ' })
        }
    }
}

private fun fix_all_cls_files_test_automatically() {
    for (kotlinPath in kotlinSources()) {
        val astPath = Path.of(kotlinPath.toString().replaceFirst("\\.kts?$".toRegex(), ".txt"))
        val actualAst = AstPrinter.txtPrint(parse(kotlinPath).psiFile)
        Files.write(astPath, actualAst.toByteArray(StandardCharsets.UTF_8))
    }
}

private fun kotlinSources(): List<Path> {
    Files.walk(Path.of("src", "test", "resources", "ast")).use { pathStream ->
        return pathStream
            .filter { path: Path ->
                !path.toFile().isDirectory && path.fileName.toString()
                    .endsWith(".kt") || path.fileName.toString().endsWith(".kts")
            }
            .sorted()
            .collect(Collectors.toList())
    }
}

private fun parse(path: Path): KotlinTree {
    val code = String(Files.readAllBytes(path), StandardCharsets.UTF_8)
    return try {
        KotlinTree.of(code, environment)
    } catch (e: ParseException) {
        throw ParseException(e.message + " in file " + path, e.position, e)
    } catch (e: RuntimeException) {
        throw RuntimeException(e.javaClass.simpleName + ": " + e.message + " in file " + path, e)
    }
}
