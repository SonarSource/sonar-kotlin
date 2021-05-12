package org.sonarsource.kotlin.checks

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.sonarsource.kotlin.plugin.KotlinCheckList
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.isRegularFile
import kotlin.streams.asSequence

class CheckConfigurationIntegrityTest {

    companion object {
        private val CHECKS_PACKAGE_DIRECTORIES = listOf(
            Path.of("checks")
        ).map { Path.of("src", "main", "java", "org", "sonarsource", "kotlin").resolve(it) }
    }

    @Test
    fun `ensure all checks are actually registered in KotlinCheckList`() {
        val expectedChecks = CHECKS_PACKAGE_DIRECTORIES.flatMap { checksDir ->
            Files.walk(checksDir, 1).asSequence()
        }.filter {
            isCheckFile(it)
        }.map {
            it.fileName.toString().substringBefore(".kt")
        }

        val actualChecks = KotlinCheckList.checks().map { it.simpleName }

        Assertions.assertThat(actualChecks).containsOnlyElementsOf(expectedChecks)
    }

    @OptIn(ExperimentalPathApi::class)
    private fun isCheckFile(candidate: Path) = candidate.isRegularFile() && candidate.fileName.toString().endsWith("Check.kt")
}
