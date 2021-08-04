package org.sonarsource.kotlin.buildsrc.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.provideDelegate
import org.gradle.kotlin.dsl.repositories
import org.gradle.process.ExecResult

abstract class FetchRuleMetadata : DefaultTask() {

    @get:Input
    val ruleKey: String by project

    @TaskAction
    fun downloadMetadata(): ExecResult {
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

        return project.javaexec {
            classpath = project.files(ruleApi.resolve())
            args = listOf("generate", "-rule", ruleKey)
            mainClass.set("com.sonarsource.ruleapi.Main")
            workingDir = project.project(":sonar-kotlin-plugin").projectDir
        }
    }
}
