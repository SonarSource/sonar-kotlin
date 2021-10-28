package org.sonarsource.kotlin.buildsrc.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.provideDelegate
import org.sonarsource.kotlin.buildsrc.tasks.Templates.checkListFile
import org.sonarsource.kotlin.buildsrc.tasks.Templates.checksDir
import org.sonarsource.kotlin.buildsrc.tasks.Templates.generateCheckClass
import org.sonarsource.kotlin.buildsrc.tasks.Templates.generateCheckFile
import org.sonarsource.kotlin.buildsrc.tasks.Templates.generateTestClass
import org.sonarsource.kotlin.buildsrc.tasks.Templates.samplesDir
import org.sonarsource.kotlin.buildsrc.tasks.Templates.testsDir
import java.nio.file.Path
import java.util.Calendar
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createFile
import kotlin.io.path.notExists
import kotlin.io.path.readLines
import kotlin.io.path.readText
import kotlin.io.path.writeLines
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
abstract class CreateRuleStubsTask : DefaultTask() {

    @get:Input
    val ruleKey: String by project

    @get:Input
    val className: String by project

    @get:Input
    @get:Optional
    val message: String? by project

    @TaskAction
    fun execute() {
        val checkClassName = if (className.endsWith("Check")) {
            className
        } else {
            logger.warn("""NOTE: Automatically adding "-Check" to the end of the className""")
            "${className}Check"
        }

        logger.info("Using properties: ")
        logger.info("  ruleKey: $ruleKey")
        logger.info("  className: $checkClassName")
        logger.info("  message: $message")

        val finishedTasks = listOf(
            "Create Check Class" to createCheckClass(checkClassName),
            "Create Test Class" to createTestClass(checkClassName),
            "Create Sample File" to createSampleFile(checkClassName),
            "Add Rule to KotlinCheckList" to addRuleToChecksListFile(checkClassName),
        )

        logger.info("--------- Done ---------")
        finishedTasks.forEach { logger.info("${it.first}: ${if (it.second) "successful" else "FAILED"}") }
    }

    private fun createCheckClass(checkClassName: String) =
        createNewFile(
            checksDir.resolve("${checkClassName}.kt"),
            generateCheckClass(ruleKey, checkClassName, message?.let { """private const val MESSAGE = "$it"""" })
        )

    private fun createTestClass(checkClassName: String): Boolean {
        val testClassName = "${checkClassName}Test"
        return createNewFile(testsDir.resolve("${testClassName}.kt"), generateTestClass(testClassName, checkClassName))
    }

    private fun createSampleFile(checkClassName: String) =
        createNewFile(samplesDir.resolve("${checkClassName}Sample.kt"), generateCheckFile("${checkClassName}Sample", ruleKey))

    private fun createNewFile(targetFile: Path, content: String) =
        if (targetFile.notExists()) {
            targetFile.createFile()
            targetFile.writeText(content)
            true
        } else {
            logger.warn("WARNING: File '$targetFile' exists. Not creating a new one.")
            false
        }

    private fun addRuleToChecksListFile(checkClassName: String): Boolean {
        val read = checkListFile.readLines()
        val toWrite = mutableListOf<String>()
        var fileSegment = 0
        val importStatementToAdd = "import org.sonarsource.kotlin.checks.$checkClassName"
        val checkListEntryToAdd = "    $checkClassName::class.java,"
        for (i in 0 until read.size - 1) {
            val currentLine = read[i]
            val nextLine = read[i + 1]
            when (fileSegment) {
                0 -> { // Header before imports
                    toWrite.add(currentLine)
                    if (nextLine.startsWith("import ")) fileSegment++
                }
                1 -> { // imports
                    if (currentLine == importStatementToAdd) {
                        // In case the import is already in the list don't add it a second time
                        fileSegment++
                    } else if (currentLine > importStatementToAdd || !currentLine.startsWith("import ")) {
                        // Add import to the list, in alphabetical ordering
                        toWrite.add(importStatementToAdd)
                        fileSegment++
                    }
                    toWrite.add(currentLine)
                }
                2 -> { // in between imports & check list
                    toWrite.add(currentLine)
                    if (nextLine.contains("::class.java,")) fileSegment++
                }
                3 -> { // check list
                    if (currentLine == checkListEntryToAdd) {
                        // In case the check is already in the list don't add it a second time
                        fileSegment++
                    } else if (currentLine > checkListEntryToAdd || !currentLine.contains("::class.java")) {
                        // Add check to the list, in alphabetical ordering
                        toWrite.add(checkListEntryToAdd)
                        fileSegment++
                    }
                    toWrite.add(currentLine)
                }
                else -> toWrite.add(currentLine)
            }
        }
        toWrite.add(read.last())

        checkListFile.writeLines(toWrite)

        return fileSegment == 4
    }
}

private object Templates {
    val checksDir = Path.of("sonar-kotlin-plugin", "src", "main", "java", "org", "sonarsource", "kotlin", "checks")
    val testsDir = Path.of("sonar-kotlin-plugin", "src", "test", "java", "org", "sonarsource", "kotlin", "checks")
    val samplesDir = Path.of("kotlin-checks-test-sources", "src", "main", "kotlin", "checks")
    val checkListFile =
        Path.of("sonar-kotlin-plugin", "src", "main", "java", "org", "sonarsource", "kotlin", "plugin", "KotlinCheckList.kt")

    @OptIn(ExperimentalPathApi::class)
    val LICENSE_HEADER by lazy {
        Path.of("LICENSE_HEADER").readText()
            .replace("${"$"}YEAR", Calendar.getInstance().get(Calendar.YEAR).toString())
    }

    fun generateCheckClass(ruleKey: String, checkClassName: String, messageLine: String?) = LICENSE_HEADER + """
        package org.sonarsource.kotlin.checks

        import org.sonar.check.Rule
        import org.sonarsource.kotlin.api.AbstractCheck
        ${messageLine?.let { "        \n        $it\n" } ?: ""}
        @Rule(key = "$ruleKey")
        class $checkClassName : AbstractCheck() {
            // TODO: implement this rule
        }
        
    """.trimIndent()

    fun generateTestClass(testClassName: String, checkClassName: String) = LICENSE_HEADER + """
        package org.sonarsource.kotlin.checks

        internal class $testClassName : CheckTest($checkClassName())
        
    """.trimIndent()

    fun generateCheckFile(checkSampleName: String, ruleKey: String) = """
        package checks
        
        class $checkSampleName {
            // TODO: insert sample code to test rule $ruleKey here
        }
        
    """.trimIndent()
}
