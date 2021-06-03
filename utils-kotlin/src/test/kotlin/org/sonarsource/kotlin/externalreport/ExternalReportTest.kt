package org.sonarsource.kotlin.externalreport

import org.assertj.core.api.Assertions
import kotlin.io.path.createFile
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteExisting
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText

fun getActual(mainMethod: (String) -> Unit): String {
    val tmpDir = createTempDirectory()
    Assertions.assertThat(tmpDir.listDirectoryEntries()).isEmpty()
    val tmpFile = tmpDir.resolve("rules-test.json")
    tmpFile.createFile()

    mainMethod(tmpFile.toString())

    val actual = tmpFile.readText()

    tmpFile.deleteExisting()
    tmpDir.deleteExisting()

    return actual
}
