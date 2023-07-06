package org.sonarsource.kotlin.buildsrc.tasks

import java.nio.file.Path

abstract class CreateKotlinRuleStubsTask : CreateRuleStubsTask(
    object : RuleStubTemplates {
        override val checksDir = Path.of("sonar-kotlin-checks", "src", "main", "java", "org", "sonarsource", "kotlin", "checks")
        override val testsDir = Path.of("sonar-kotlin-checks", "src", "test", "java", "org", "sonarsource", "kotlin", "checks")
        override val samplesDir = Path.of("kotlin-checks-test-sources", "src", "main", "kotlin", "checks")
        override val checkListFile =
            Path.of("sonar-kotlin-plugin", "src", "main", "java", "org", "sonarsource", "kotlin", "plugin", "KotlinCheckList.kt")
        override val checksPackage = "org.sonarsource.kotlin.checks"
        override val sampleFileExt = "kt"

        override fun generateCheckFile(checkSampleName: String, ruleKey: String) = """
            package checks
            
            class $checkSampleName {
                // TODO: insert sample code to test rule $ruleKey here
            }
            
        """.trimIndent()
    }
)