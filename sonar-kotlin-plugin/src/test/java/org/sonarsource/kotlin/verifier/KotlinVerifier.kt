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
package org.sonarsource.kotlin.verifier

import io.mockk.InternalPlatformDsl.toStr
import org.sonarsource.analyzer.commons.checks.verifier.SingleFileVerifier
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.converter.KotlinConverter
import org.sonarsource.slang.api.ASTConverter
import org.sonarsource.slang.api.Comment
import org.sonarsource.slang.api.TopLevelTree
import org.sonarsource.slang.checks.api.SlangCheck
import org.sonarsource.slang.testing.Verifier
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

class KotlinVerifier(private val check: AbstractCheck) {
    var fileName: String = ""
    var classpath: List<String> = System.getProperty("java.class.path").split(":") + listOf(KOTLIN_CLASSPATH)
    var deps: List<String> = getClassPath(DEFAULT_TEST_JARS_DIRECTORY)

    fun verify() {
        val converter = KotlinConverter(classpath + deps)
        createVerifier(converter, KOTLIN_BASE_DIR.resolve(fileName), check)
            .assertOneOrMoreIssues()
    }

    fun verifyNoIssue() {
        val converter = KotlinConverter(classpath + deps)
        createVerifier(converter, KOTLIN_BASE_DIR.resolve(fileName), check)
            .assertNoIssues()
    }

    private fun createVerifier(
        converter: ASTConverter,
        path: Path,
        check: AbstractCheck,
    ): SingleFileVerifier {
        val verifier = SingleFileVerifier.create(path, StandardCharsets.UTF_8)
        val testFileContent = String(Files.readAllBytes(path), StandardCharsets.UTF_8)
        val root = converter.parse(testFileContent, null)
        (root as TopLevelTree).allComments()
            .forEach { comment: Comment ->
                val start = comment.textRange().start()
                verifier.addComment(start.line(), start.lineOffset() + 1, comment.text(), 2, 0)
            }
        val ctx = TestContext(verifier, check)
        ctx.scan(root)
        return verifier
    }

    companion object {
        private val BASE_DIR = Paths.get("src", "test", "resources", "checks")
        private val KOTLIN_BASE_DIR = Paths.get("..", "kotlin-checks-test-sources", "src", "main", "kotlin", "checks")
        private val KOTLIN_CLASSPATH = "../kotlin-checks-test-sources/build/classes"
        private val DEFAULT_TEST_JARS_DIRECTORY = "../kotlin-checks-test-sources/build/test-jars"
        private val CONVERTER: ASTConverter = KotlinConverter(emptyList())

        @JvmStatic
        @Deprecated("Use KotlinVerifier#verify for testing KotlinChecks instead.")
        fun verify(fileName: String, check: SlangCheck) {
            Verifier.verify(CONVERTER, BASE_DIR.resolve(fileName), check)
        }
    }

    private fun getClassPath(jarsDirectory: String): List<String> {
        var classpath = mutableListOf<String>()
        val testJars = Paths.get(jarsDirectory)
        if (testJars.toFile().exists()) {
            classpath = getFilesRecursively(testJars)
        } else if (DEFAULT_TEST_JARS_DIRECTORY != jarsDirectory) {
            throw AssertionError(
                "The directory to be used to extend class path does not exists (${testJars.toAbsolutePath()})."
            )
        }
        return classpath
    }

    private fun getFilesRecursively(root: Path): MutableList<String> {
        val files: MutableList<String> = ArrayList()
        val visitor: FileVisitor<Path> = object : SimpleFileVisitor<Path>() {
            override fun visitFile(filePath: Path, attrs: BasicFileAttributes): FileVisitResult {
                files.add(filePath.toStr())
                return FileVisitResult.CONTINUE
            }

            override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
                return FileVisitResult.CONTINUE
            }
        }
        Files.walkFileTree(root, visitor)
        return files
    }
}

fun KotlinVerifier(check: AbstractCheck, block: KotlinVerifier.() -> Unit) =
    KotlinVerifier(check)
        .apply(block)

