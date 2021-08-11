package org.sonarsource.kotlin.checks

import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.verifier.KotlinVerifier

private const val NO_SEMANTICS_TEST_FILE_POSTFIX = "SampleNoSemantics.kt"

abstract class CheckTestWithNoSemantics(
    check: AbstractCheck,
    sampleFileSemantics: String? = null,
    val sampleFileNoSemantics: String? = null,
    classpath: List<String>? = null,
    dependencies: List<String>? = null,
    val shouldReport: Boolean = false,
) : CheckTest(
    check = check,
    sampleFileSemantics = sampleFileSemantics,
    classpath = classpath,
    dependencies = dependencies
) {
    @Test
    fun `without semantics`() {
        KotlinVerifier(check) {
            this.fileName = sampleFileNoSemantics ?: "$checkName$NO_SEMANTICS_TEST_FILE_POSTFIX"
            this.classpath = this@CheckTestWithNoSemantics.classpath ?: emptyList()
            this.deps = this@CheckTestWithNoSemantics.dependencies ?: emptyList()
        }.let { 
            if (this.shouldReport) it.verify() else it.verifyNoIssue()
        }
    }
}
