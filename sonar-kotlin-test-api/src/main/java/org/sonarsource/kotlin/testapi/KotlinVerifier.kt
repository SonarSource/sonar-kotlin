/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
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
package org.sonarsource.kotlin.testapi

import io.mockk.InternalPlatformDsl.toStr
import com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.LanguageVersion
import org.sonar.api.batch.fs.InputFile
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonarsource.analyzer.commons.checks.verifier.SingleFileVerifier
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.api.frontend.Environment
import org.sonarsource.kotlin.api.frontend.KotlinTree
import org.sonarsource.kotlin.api.visiting.Comment
import org.sonarsource.kotlin.api.visiting.CommentAnnotationsAndTokenVisitor
import java.io.File
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import kotlin.io.path.extension
import kotlin.io.path.pathString

val KOTLIN_BASE_DIR = Paths.get("..", "kotlin-checks-test-sources", "src", "main", "kotlin", "checks")
val DEFAULT_KOTLIN_CLASSPATH = listOf("../kotlin-checks-test-sources/build/classes/kotlin/main", "../kotlin-checks-test-sources/build/classes/java/main")
private val DEFAULT_TEST_JARS_DIRECTORY = "../kotlin-checks-test-sources/build/test-jars"

class KotlinVerifier(private val check: AbstractCheck) {

    var fileName: String = ""
    var baseDir: Path = KOTLIN_BASE_DIR
    var classpath: List<String> = System.getProperty("java.class.path").split(File.pathSeparatorChar) + DEFAULT_KOTLIN_CLASSPATH
    var deps: List<String> = getClassPath(DEFAULT_TEST_JARS_DIRECTORY)
    var isAndroid = false

    fun verify() {
        verifyFile {
            assertOneOrMoreIssues()
        }
    }

    fun verifyNoIssue() {
        verifyFile {
            assertNoIssues()
        }
    }
    private fun verifyFile(verify: SingleFileVerifier.() -> Unit) {
        val filePath = baseDir.resolve(fileName)

        val disposable = Disposer.newDisposable()
        val environment = Environment(disposable, classpath + deps, LanguageVersion.LATEST_STABLE)
        val converter = { content: String ->
            val inputFile = TestInputFileBuilder("moduleKey", filePath.fileName.pathString)
                .setCharset(StandardCharsets.UTF_8)
                .setModuleBaseDir(baseDir)
                .initMetadata(content).build()

            kotlinTreeOf(content, environment, inputFile) to inputFile
        }
        try {
            createVerifier(converter, filePath).verify()
        } finally {
            Disposer.dispose(disposable)
        }
    }


    private fun createVerifier(
        converter: (String) -> Pair<KotlinTree, InputFile>,
        path: Path
    ): SingleFileVerifier {
        val verifier = SingleFileVerifier.create(path, StandardCharsets.UTF_8)

        val testFileContent = String(Files.readAllBytes(path), StandardCharsets.UTF_8)
        val (root, inputFile) = converter(testFileContent)

        CommentAnnotationsAndTokenVisitor(root.document, inputFile).apply { visitElement(root.psiFile) }.allComments
            .forEach { comment: Comment ->
                val start = comment.range.start()
                verifier.addComment(start.line(), start.lineOffset() + 1, comment.text, 2, 0)
            }
        val ctx = TestContext(verifier, check, inputFile = DummyInputFile(path), isAndroid = isAndroid)
        ctx.scan(root)
        return verifier
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

