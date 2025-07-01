package org.sonarsource.kotlin.buildsrc.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.repositories
import org.gradle.process.ExecResult

const val ruleApiVersion = "2.12.0.4409"

abstract class FetchRuleMetadata : DefaultTask() {

    private fun addRuleApiToProjectConfig(): Configuration {
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
            ruleApi("com.sonarsource.rule-api:rule-api:$ruleApiVersion")
        }

        return ruleApi
    }

    internal fun executeRuleApi(arguments: List<String>): ExecResult {
        val ruleApi = addRuleApiToProjectConfig()
        return project.javaexec {
            classpath = project.files(ruleApi.resolve())
            args = arguments
            mainClass.set("com.sonarsource.ruleapi.Main")
            workingDir = project.project(":sonar-kotlin-plugin").projectDir
        }
    }

    abstract class FetchSpecificRulesMetadata : FetchRuleMetadata() {
        @get:Input
        val ruleKey: String by project

        @get:Input
        @get:Optional
        val branch: String? by project

        @get:Input
        @get:Optional
        val autoSelectBranch: String? by project

        @TaskAction
        fun downloadMetadata() {
            val finalRspecBranch = if (branch == null) {
                kotlin.runCatching {
                    GitHubApi.getBranchCandidatesForRule(ruleKey)
                }.onFailure { e ->
                    System.err.println("Cannot query GitHub for metadata branch candidates:\n$e")
                }.getOrNull()?.let { prs ->
                    if (prs.isEmpty()) return@let null
                    else if (autoSelectBranch == "true" || autoSelectBranch == "yes" || autoSelectBranch?.isEmpty() == true) {
                        return@let prs.first().head.ref
                    }

                    val optionsText = prs.mapIndexed { i, pr ->
                        "  ${i + 1}. ${pr.head.ref} - #${pr.number}: ${pr.title} (${pr.html_url})"
                    }

                    var selection: Int? = null
                    while (selection == null || selection !in 0..optionsText.size) {
                        println("""
                            


                            RSPEC branches mentioning this rule were found. Please select which branch to update from:
                              0. master
                            ${optionsText.joinToString("\n")}
                            
                            Selection [1]: 
                        """.trimIndent())

                        val userInput = readLine()

                        if (userInput.isNullOrBlank()) {
                            selection = 1
                        } else {
                            selection = runCatching { userInput.toInt() }.getOrNull()
                        }
                    }

                    if (selection > 0) prs[selection - 1].head.ref
                    else null
                }
            } else branch

            executeRuleApi(listOf("generate", "-rule", ruleKey) + (finalRspecBranch?.let { listOf("-branch", it) } ?: emptyList()))
        }
    }

    abstract class FetchAllRulesMetadata : FetchRuleMetadata() {
        @TaskAction
        fun downloadMetadata() = executeRuleApi(listOf("update"))
    }
}
