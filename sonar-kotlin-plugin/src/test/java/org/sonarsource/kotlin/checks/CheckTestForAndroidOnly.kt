package org.sonarsource.kotlin.checks

import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.verifier.KotlinVerifier

private const val NON_ANDROID_TEST_FILE_POSTFIX = "SampleNonAndroid.kt"

abstract class CheckTestForAndroidOnly(
    check: AbstractCheck,
    sampleFileSemantics: String? = null,
    val sampleFileNonAndroid: String? = null,
    classpath: List<String>? = null,
    dependencies: List<String>? = null,
) : CheckTest(
    check = check,
    sampleFileSemantics = sampleFileSemantics,
    classpath = classpath,
    dependencies = dependencies,
    isAndroid = true,
) {
    @Test
    fun `non Android`() {
        KotlinVerifier(check) {
            this.fileName = sampleFileNonAndroid ?: "$checkName$NON_ANDROID_TEST_FILE_POSTFIX"
            this.isAndroid = false
        }.verifyNoIssue()
    }
}
