package org.sonarsource.kotlin.buildsrc.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.provideDelegate

import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createFile
import kotlin.io.path.notExists
import kotlin.io.path.readLines
import kotlin.io.path.writeLines
import kotlin.io.path.writeText

@OptIn(ExperimentalPathApi::class)
abstract class CreateRuleStubsTask internal constructor(
    private val templates: RuleStubTemplates
) : DefaultTask() {

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
        logger.info("  templates: ${templates::class.simpleName}")

        val finishedTasks = listOf(
            "Create Check Class" to createCheckClass(checkClassName),
            "Create Test Class" to createTestClass(checkClassName),
            "Create Sample File" to templates.createSampleFile(checkClassName),
            "Add Rule to KotlinCheckList" to templates.addRuleToChecksListFile(checkClassName),
        )

        logger.info("--------- Done ---------")
        finishedTasks.forEach { logger.info("${it.first}: ${if (it.second) "successful" else "FAILED"}") }
    }

    private fun createCheckClass(checkClassName: String) = createNewFile(
        templates.checksDir.resolve("${checkClassName}.kt"),
        generateCheckClass(ruleKey, checkClassName, message?.let { """private const val MESSAGE = "$it"""" })
    )

    private fun createTestClass(checkClassName: String): Boolean {
        val testClassName = "${checkClassName}Test"
        return createNewFile(templates.testsDir.resolve("${testClassName}.kt"), generateTestClass(testClassName, checkClassName))
    }

    private fun generateCheckClass(ruleKey: String, checkClassName: String, messageLine: String?) = LICENSE_HEADER + """
            package ${templates.checksPackage}

            import org.sonar.check.Rule
            import org.sonarsource.kotlin.api.checks.AbstractCheck
            ${messageLine?.let { "        \n        $it\n" } ?: ""}
            @Rule(key = "$ruleKey")
            class $checkClassName : AbstractCheck() {
                // TODO: implement this rule
            }
            
        """.trimIndent()

    private fun generateTestClass(testClassName: String, checkClassName: String) = LICENSE_HEADER + """
            package ${templates.checksPackage}
    
            internal class $testClassName : CheckTest($checkClassName())

        """.trimIndent()

    private fun RuleStubTemplates.createSampleFile(checkClassName: String) =
        createNewFile(samplesDir.resolve("${checkClassName}Sample.$sampleFileExt"), generateCheckFile("${checkClassName}Sample", ruleKey))

    private fun createNewFile(targetFile: Path, content: String) = if (targetFile.notExists()) {
        targetFile.createFile()
        targetFile.writeText(content)
        true
    } else {
        logger.warn("WARNING: File '$targetFile' exists. Not creating a new one.")
        false
    }

    private fun RuleStubTemplates.addRuleToChecksListFile(checkClassName: String): Boolean {
        val read = checkListFile.readLines()
        val toWrite = mutableListOf<String>()
        var fileSegment = 0
        val importStatementToAdd = "import $checksPackage.$checkClassName"
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
