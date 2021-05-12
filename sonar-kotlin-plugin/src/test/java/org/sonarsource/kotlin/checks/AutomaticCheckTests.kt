package org.sonarsource.kotlin.checks

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.sonarsource.kotlin.api.AbstractCheck
import org.sonarsource.kotlin.plugin.KotlinCheckList
import org.sonarsource.kotlin.verifier.KotlinVerifier
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.isRegularFile
import kotlin.streams.asSequence

data class TestConfiguration(
    val check: Class<out AbstractCheck>,
    val testFileName: String = "${check.simpleName}Sample.kt",
    val classpath: List<String>? = null,
    val dependencies: List<String>? = null,
    val testName: String = check.simpleName,
)

class AutomaticCheckTests {
    companion object {

        private val OVERRIDDEN_CONFIG: Set<TestConfiguration> = setOf()

        private val OVERRIDDEN_CONFIG_WITHOUT_SEMANTICS: Set<TestConfiguration> = setOf()

        private const val NO_SEMANTICS_TEST_FILE_POSTFIX = "SampleNoSemantics.kt"

        @JvmStatic
        fun checks(): List<Arguments> {
            val checksWithOverriddenTestsConfig = OVERRIDDEN_CONFIG.map { it.check }

            return (OVERRIDDEN_CONFIG +
                KotlinCheckList.checks()
                    .filter { it !in checksWithOverriddenTestsConfig }
                    .map { check -> TestConfiguration(check) }
                ).map { Arguments.of(it, it.testName) }
        }

        @JvmStatic
        fun checksWithoutSemantics(): List<Arguments> {
            val checksWithOverriddenTestsConfig = OVERRIDDEN_CONFIG_WITHOUT_SEMANTICS.map { it.check }
            val checkNamesWithAvailableSampleTestFiles = Files.walk(KotlinVerifier.KOTLIN_BASE_DIR).asSequence()
                .filter { isTestFileWithoutSemantics(it) }
                .map { it.fileName.toString().substringBefore(NO_SEMANTICS_TEST_FILE_POSTFIX) }
                .toList()

            val checksWithSamples = KotlinCheckList.checks().filter {
                it.simpleName in checkNamesWithAvailableSampleTestFiles
            }

            assertThat(checksWithSamples.map { it.simpleName }).containsOnlyElementsOf(checkNamesWithAvailableSampleTestFiles)

            return (OVERRIDDEN_CONFIG_WITHOUT_SEMANTICS + (checksWithSamples - checksWithOverriddenTestsConfig).map { check ->
                TestConfiguration(check, "${check.simpleName}$NO_SEMANTICS_TEST_FILE_POSTFIX")
            }).map { Arguments.of(it, it.testName) }
        }

        @OptIn(ExperimentalPathApi::class)
        private fun isTestFileWithoutSemantics(candidate: Path) =
            candidate.isRegularFile() && candidate.fileName.toString().endsWith(NO_SEMANTICS_TEST_FILE_POSTFIX)
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("checks")
    fun `automatically test all registered Kotlin tests`(testConfig: TestConfiguration, testName: String) {
        val (check, fileName, classpath, dependencies, _) = testConfig

        KotlinVerifier(check.getDeclaredConstructor().newInstance()) {
            this.fileName = fileName
            classpath?.let { this.classpath = it }
            dependencies?.let { this.deps = it }
        }.verify()
    }

    @ParameterizedTest(name = "{1} without semantics")
    @MethodSource("checksWithoutSemantics")
    fun `automatically test all registered Kotlin tests without semantics where available`(
        testConfig: TestConfiguration, testName: String,
    ) {
        KotlinVerifier(testConfig.check.getDeclaredConstructor().newInstance()) {
            fileName = testConfig.testFileName
            classpath = testConfig.classpath ?: emptyList()
            deps = testConfig.dependencies ?: emptyList()
        }.verifyNoIssue()
    }
}
