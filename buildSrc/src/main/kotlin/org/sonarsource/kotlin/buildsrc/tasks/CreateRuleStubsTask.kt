package org.sonarsource.kotlin.buildsrc.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.repositories
import org.sonarsource.kotlin.buildsrc.tasks.Templates.checkListFile
import org.sonarsource.kotlin.buildsrc.tasks.Templates.checksDir
import org.sonarsource.kotlin.buildsrc.tasks.Templates.generateCheckClass
import org.sonarsource.kotlin.buildsrc.tasks.Templates.generateCheckFile
import org.sonarsource.kotlin.buildsrc.tasks.Templates.generateTestClass
import org.sonarsource.kotlin.buildsrc.tasks.Templates.samplesDir
import org.sonarsource.kotlin.buildsrc.tasks.Templates.testsDir
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createFile
import kotlin.io.path.notExists
import kotlin.io.path.readLines
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
            "Download Metadata" to downloadMetadata(),
        )

        logger.info("--------- Done ---------")
        finishedTasks.forEach { logger.info("${it.first}: ${if (it.second) "successful" else "FAILED"}") }
    }

    private fun createCheckClass(checkClassName: String): Boolean {
        val checkFile = checksDir.resolve("${checkClassName}.kt")
        val messageLine = message?.let { """private const val MESSAGE = "$it"""" }
        return if (checkFile.notExists()) {
            checkFile.createFile()
            checkFile.writeText(generateCheckClass(ruleKey, checkClassName, messageLine))
            true
        } else {
            logger.warn("WARNING: Check file '$checkFile' exists. Not creating a new one.")
            false
        }
    }

    private fun createTestClass(checkClassName: String): Boolean {
        val testClassName = "${checkClassName}Test"
        val testFile = testsDir.resolve("${testClassName}.kt")
        return if (testFile.notExists()) {
            testFile.createFile()
            testFile.writeText(generateTestClass(testClassName, checkClassName))
            true
        } else {
            logger.warn("WARNING: Test file '$testFile' exists. Not creating a new one.")
            false
        }
    }

    private fun createSampleFile(checkClassName: String): Boolean {
        val sampleClassName = "${checkClassName}Sample"
        val sampleFile = samplesDir.resolve("${sampleClassName}.kt")
        return if (sampleFile.notExists()) {
            sampleFile.createFile()
            sampleFile.writeText(generateCheckFile(ruleKey))
            true
        } else {
            logger.warn("WARNING: Sample file '$sampleFile' exists. Not creating a new one.")
            false
        }
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

    private fun downloadMetadata(): Boolean {
        project.repositories {
            maven {
                url = project.uri("https://repox.jfrog.io/repox/sonarsource-private-releases")
                authentication {
                    credentials {
                        val artifactoryUsername: String by project
                        val artifactoryPassword: String by project
                        username = artifactoryUsername
                        password = artifactoryPassword
                    }
                }
            }
        }

        val ruleApi = project.configurations.create("ruleApi")
        project.dependencies {
            ruleApi("com.sonarsource.rule-api:rule-api:2.0.0.1885")
        }

        val exec = project.javaexec {
            classpath = project.files(ruleApi.resolve())
            args = listOf("generate", "-rule", ruleKey)
            mainClass.set("com.sonarsource.ruleapi.Main")
            workingDir = project.project(":sonar-kotlin-plugin").projectDir
        }

        return exec.exitValue == 0
    }
}

private object Templates {
    val checksDir = Path.of("sonar-kotlin-plugin", "src", "main", "java", "org", "sonarsource", "kotlin", "checks")
    val testsDir = Path.of("sonar-kotlin-plugin", "src", "test", "java", "org", "sonarsource", "kotlin", "checks")
    val samplesDir = Path.of("kotlin-checks-test-sources", "src", "main", "kotlin", "checks")
    val checkListFile =
        Path.of("sonar-kotlin-plugin", "src", "main", "java", "org", "sonarsource", "kotlin", "plugin", "KotlinCheckList.kt")

    val HEADER = """/*
         * SonarSource SLang
         * Copyright (C) 2018-2021 SonarSource SA
         * mailto:info AT sonarsource DOT com
         *
         * This program is free software; you can redistribute it and/or
         * modify it under the terms of the GNU Lesser General Public
         * License as published by the Free Software Foundation; either
         * version 3 of the License, or (at your option) any later version.
         *
         * This program is distributed in the hope that it will be useful,
         * but WITHOUT ANY WARRANTY; without even the implied warranty of
         * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
         * Lesser General Public License for more details.
         *
         * You should have received a copy of the GNU Lesser General Public License
         * along with this program; if not, write to the Free Software Foundation,
         * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
         */"""

    fun generateCheckClass(ruleKey: String, checkClassName: String, messageLine: String?) = """
        $HEADER
        package org.sonarsource.kotlin.checks

        import org.sonar.check.Rule
        import org.sonarsource.kotlin.api.AbstractCheck
        ${messageLine?.let { "        \n        $it\n" } ?: ""}
        @Rule(key = "$ruleKey")
        class $checkClassName : AbstractCheck() {
            // TODO: implement this rule
        }
    """.trimIndent()

    fun generateTestClass(testClassName: String, checkClassName: String) = """
        $HEADER
        package org.sonarsource.kotlin.checks

        class $testClassName : CheckTest($checkClassName())
    """.trimIndent()

    fun generateCheckFile(ruleKey: String) = """
        package checks

        // TODO: insert sample code to test rule $ruleKey here
    """.trimIndent()
}
