package org.sonarsource.kotlin.checks

import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.verifier.KotlinVerifier

private const val TEST_FILE_POSTFIX = "Sample.kt"

abstract class CheckTest(
    val check: AbstractCheck,
    val sampleFileSemantics: String? = null,
    val classpath: List<String>? = null,
    val dependencies: List<String>? = null,
    val isAndroid: Boolean = false
) {
    protected val checkName = check::class.java.simpleName

    @Test
    fun `with semantics`() {
        KotlinVerifier(check) {
            this.fileName = sampleFileSemantics ?: "$checkName$TEST_FILE_POSTFIX"
            this@CheckTest.classpath?.let { this.classpath = it }
            this@CheckTest.dependencies?.let { this.deps = it }
            this.isAndroid = this@CheckTest.isAndroid
        }.verify()
    }
}
