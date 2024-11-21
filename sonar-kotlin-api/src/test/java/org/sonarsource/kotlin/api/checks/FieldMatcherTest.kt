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
package org.sonarsource.kotlin.api.checks

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.junit.jupiter.api.Test
import org.sonar.api.batch.fs.internal.DefaultInputFile
import org.sonar.api.batch.fs.internal.TestInputFileBuilder
import org.sonarsource.kotlin.api.frontend.Environment
import org.sonarsource.kotlin.testapi.kotlinTreeOf
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class FieldMatcherTest {

    val environment = Environment(listOf("../kotlin-checks-test-sources/build/classes/kotlin/main"), LanguageVersion.LATEST_STABLE)

    val path: Path = Paths.get("../kotlin-checks-test-sources/src/main/kotlin/sample/fields.kt")

    val content = String(Files.readAllBytes(path))

    val inputFile: DefaultInputFile = TestInputFileBuilder("moduleKey", "src/org/foo/kotlin.kt")
        .setCharset(StandardCharsets.UTF_8)
        .initMetadata(content)
        .build()

    private val tree = kotlinTreeOf(content, environment, inputFile)

    private val fieldReferences = tree.psiFile.collectDescendantsOfType<KtNameReferenceExpression>().rearrange(
        11, // 0: string.length
        13, // 1: collection.size
        17, // 2: list.size
        19, // 3: customizedList.size
        21, // 4: customizedList.zoodles
        23, // 5: nonCollection.size
        25, // 6: sample.name
        27, // 7: sample.pi
        29, // 8: listContainer.customizedList
        32, // 9: listContainer.customizedList.size
        36, // 10: listContainer.listContainer.customizedList.size
    )

    @Test
    fun `match field by name`() {
        check(FieldMatcher {
            withNames("size")
        }, 1, 2, 3, 5, 9, 10)

        check(FieldMatcher {
            withNames("zoodles")
        }, 4)

        check(FieldMatcher {
            withNames("zonkers")
        })

        check(FieldMatcher {
            withNames("zoodles", "zonker", "pi")
        }, 4, 7)
    }

    @Test
    fun `match field by defining type`() {
        check(FieldMatcher {
            withDefiningTypes("kotlin.collections.Collection")
        }, 1, 2, 3, 4, 9, 10)

        check(FieldMatcher {
            withDefiningTypes("kotlin.collections.List")
        }, 2, 3, 4, 9, 10)

        check(FieldMatcher {
            withDefiningTypes("sample.CustomizedList")
        }, 3, 4, 9, 10)

        check(FieldMatcher {
            withDefiningTypes("sample.Sample")
        }, 6, 7)

        check(FieldMatcher {
            withDefiningTypes("sample.Sumple")
        })
    }

    @Test
    fun `match field by qualifier`() {
        check(FieldMatcher {
            withQualifiers("kotlin.collections.Collection")
        }, 1)

        check(FieldMatcher {
            withQualifiers("kotlin.collections.List")
        }, 2)

        check(FieldMatcher {
            withQualifiers("sample.CustomizedList")
        }, 3, 4, 9, 10)

        check(FieldMatcher {
            withQualifiers("sample.Sample")
        }, 6, 7)

        check(FieldMatcher {
            withQualifiers("sample.Sumple")
        })
    }

    @Test
    fun `match field by name and defining type`() {
        check(FieldMatcher {
            withNames("pi")
            withDefiningTypes("sample.Sample")
        }, 7)

        check(FieldMatcher {
            withNames("size")
            withDefiningTypes("kotlin.collections.List")
        }, 2, 3, 9, 10)

        check(FieldMatcher {
            withNames("size")
            withDefiningTypes("sample.CustomizedList")
        }, 3, 9, 10)

        check(FieldMatcher {
            withNames("size")
            withDefiningTypes("sample.NonCollection")
        }, 5)
    }

    @Test
    fun `empty matcher`() {
        check(FieldMatcher {}, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    }

    private fun check(matcher: FieldMatcher, vararg expectedMatches: Int) {
        assertThat(
            fieldReferences.mapIndexedNotNull { index, it ->
                if (matcher.matches(it, tree.bindingContext)) index else null
            }).isEqualTo(expectedMatches.toList())
    }
}

private fun <T> List<T>.rearrange(vararg indices: Int) = List(indices.size) { this[indices[it]] }
