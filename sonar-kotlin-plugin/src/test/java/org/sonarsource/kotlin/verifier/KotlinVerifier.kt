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

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.sonarsource.analyzer.commons.checks.verifier.SingleFileVerifier
import org.sonarsource.kotlin.api.KotlinCheck
import org.sonarsource.kotlin.converter.KotlinConverter
import org.sonarsource.slang.api.ASTConverter
import org.sonarsource.slang.api.Comment
import org.sonarsource.slang.api.TopLevelTree
import org.sonarsource.slang.checks.api.SlangCheck
import org.sonarsource.slang.testing.Verifier
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class KotlinVerifier(private val check: KotlinCheck<*>) { 
    var fileName: String = ""
    var classpath: List<String> = emptyList()

    fun verify() {
        val converter = KotlinConverter(classpath)
        createVerifier(converter, BASE_DIR.resolve(fileName), check)
            .assertOneOrMoreIssues()
    }

    fun verifyNoIssue() {
        val converter = KotlinConverter(classpath)
        createVerifier(converter, BASE_DIR.resolve(fileName), check)
            .assertNoIssues()
    }

    private fun <T : PsiElement> createVerifier(converter: ASTConverter, path: Path, check: KotlinCheck<T>): SingleFileVerifier {
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
        private val CONVERTER: ASTConverter = KotlinConverter(emptyList())

        @JvmStatic
        fun verify(fileName: String, check: SlangCheck) {
            Verifier.verify(CONVERTER, BASE_DIR.resolve(fileName), check)
        }
    }
}

fun KotlinVerifier(check: KotlinCheck<*>, block: KotlinVerifier.() -> Unit) = 
    KotlinVerifier(check)
        .apply(block)
