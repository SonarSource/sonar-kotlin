package org.sonarsource.kotlin.checks

import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.verifier.KotlinVerifier
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.streams.asSequence

private val checkNamesWithAvailableSampleTestFiles by lazy {
    Files.walk(KotlinVerifier.KOTLIN_BASE_DIR).asSequence()
        .filter { isTestFileWithoutSemantics(it) }
        .map { it.fileName.toString().substringBefore(NO_SEMANTICS_TEST_FILE_POSTFIX) }
        .toSet()
}
private const val NO_SEMANTICS_TEST_FILE_POSTFIX = "SampleNoSemantics.kt"

abstract class SimpleCheckTest(
    val check: AbstractCheck,
    val sampleFileSemantics: String? = null,
    val sampleFileNoSemantics: String? = null,
    val classpath: List<String>? = null,
    val dependencies: List<String>? = null,
    val alsoTestWithoutSemantics: Boolean? = null,
) {
    private val checkName = check::class.java.simpleName

    @Test
    fun `with semantics`() {
        KotlinVerifier(check) {
            this.fileName = sampleFileSemantics ?: "${checkName}Sample.kt"
            this@SimpleCheckTest.classpath?.let { this.classpath = it }
            this@SimpleCheckTest.dependencies?.let { this.deps = it }
        }.verify()
    }

    @Test
    fun `without semantics`() {
        assumeTrue(shouldRunTestWithoutSemantics())

        KotlinVerifier(check) {
            this.fileName = sampleFileNoSemantics ?: "${checkName}SampleNoSemantics.kt"
            this.classpath = this@SimpleCheckTest.classpath ?: emptyList()
            this.deps = this@SimpleCheckTest.dependencies ?: emptyList()
        }.verifyNoIssue()
    }

    private fun shouldRunTestWithoutSemantics() = alsoTestWithoutSemantics ?: checkName in checkNamesWithAvailableSampleTestFiles
}

private fun isTestFileWithoutSemantics(candidate: Path) =
    candidate.isRegularFile() && candidate.fileName.toString().endsWith(NO_SEMANTICS_TEST_FILE_POSTFIX)
