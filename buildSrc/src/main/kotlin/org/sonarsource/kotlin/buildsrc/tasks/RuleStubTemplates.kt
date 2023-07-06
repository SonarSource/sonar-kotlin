package org.sonarsource.kotlin.buildsrc.tasks

import java.nio.file.Path

internal interface RuleStubTemplates {
    val checksDir: Path
    val testsDir: Path
    val samplesDir: Path
    val checkListFile: Path
    val checksPackage: String
    val sampleFileExt: String

    fun generateCheckFile(checkSampleName: String, ruleKey: String): String
}
