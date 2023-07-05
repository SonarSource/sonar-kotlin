package org.sonarsource.kotlin.buildsrc.tasks

import java.nio.file.Path

private const val KOTLIN_GRADLE_BASEDIR = "sonar-kotlin-gradle"

abstract class CreateKotlinGradleRuleStubsTask : CreateRuleStubsTask(
    object : RuleStubTemplates {
        override val checksDir = Path.of(KOTLIN_GRADLE_BASEDIR, "src", "main", "java", "org", "sonarsource", "kotlin", "gradle", "checks")
        override val testsDir = Path.of(KOTLIN_GRADLE_BASEDIR, "src", "test", "java", "org", "sonarsource", "kotlin", "gradle", "checks")
        override val samplesDir = Path.of(KOTLIN_GRADLE_BASEDIR, "src", "test", "samples", "non-compiling")
        override val checkListFile =
            Path.of(KOTLIN_GRADLE_BASEDIR, "src", "main", "java", "org", "sonarsource", "kotlin", "gradle", "KotlinGradleCheckList.kt")
        override val checksPackage = "org.sonarsource.kotlin.gradle.checks"
        override val sampleFileExt = "kts"

        override fun generateCheckClass(ruleKey: String, checkClassName: String, messageLine: String?) = LICENSE_HEADER + """
            package $checksPackage
            
            import org.sonar.check.Rule
            import org.sonarsource.kotlin.api.checks.AbstractCheck       
            ${messageLine?.let { "        \n        $it\n" } ?: ""}
            @Rule(key = "$ruleKey")
            class $checkClassName : AbstractCheck() {
                // TODO: implement this rule
            }
            
        """.trimIndent()

        override fun generateTestClass(testClassName: String, checkClassName: String) = LICENSE_HEADER + """
            package $checksPackage
            
            internal class $testClassName : CheckTest($checkClassName())
                    
        """.trimIndent()

        override fun generateCheckFile(checkSampleName: String, ruleKey: String) = """
            // TODO: insert sample code to test rule $ruleKey here
            
        """.trimIndent()
    }
)