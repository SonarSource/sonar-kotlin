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
package org.sonarsource.kotlin.checks

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledIf
import org.sonarsource.kotlin.api.checks.AbstractCheck
import org.sonarsource.kotlin.testapi.KotlinVerifier
import kotlin.reflect.full.hasAnnotation

private const val TEST_FILE_POSTFIX = "Sample.kt"

abstract class CheckTest(
    val check: AbstractCheck,
    val sampleFileSemantics: String? = null,
    val sampleFileK2: String? = sampleFileSemantics,
    val classpath: List<String>? = null,
    val dependencies: List<String>? = null,
    val isAndroid: Boolean = false
) {
    protected val checkName = check::class.java.simpleName

    private fun k1only() = check::class.hasAnnotation<org.sonarsource.kotlin.api.frontend.K1only>()

    @Test
    @DisabledIf("k1only")
    fun `with k2 semantics`() {
        KotlinVerifier(check) {
            this.fileName = sampleFileK2 ?: "$checkName$TEST_FILE_POSTFIX"
            this@CheckTest.classpath?.let { this.classpath = it }
            this@CheckTest.dependencies?.let { this.deps = it }
            this.useK2 = true
            this.isAndroid = this@CheckTest.isAndroid
        }.verify()
    }

    @Test
    fun `with k1 semantics`() {
        KotlinVerifier(check) {
            this.fileName = sampleFileSemantics ?: "$checkName$TEST_FILE_POSTFIX"
            this@CheckTest.classpath?.let { this.classpath = it }
            this@CheckTest.dependencies?.let { this.deps = it }
            this.isAndroid = this@CheckTest.isAndroid
        }.verify()
    }
}
