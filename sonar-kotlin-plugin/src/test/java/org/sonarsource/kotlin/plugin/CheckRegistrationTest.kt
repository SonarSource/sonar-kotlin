package org.sonarsource.kotlin.plugin

import io.mockk.spyk
import io.mockk.verify
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.junit.jupiter.api.Test
import org.sonar.api.config.internal.MapSettings
import org.sonar.api.issue.NoSonarFilter
import org.sonar.check.Rule
import org.sonarsource.kotlin.api.InitContext
import org.sonarsource.kotlin.api.KotlinCheck
import org.sonarsource.slang.plugin.InputFileContext
import org.sonarsource.slang.testing.AbstractSensorTest

class CheckRegistrationTest : AbstractSensorTest() {
    val spyVisitor = spyk({ _: InputFileContext, _: PsiElement -> })

    @Rule(key = "S99999")
    inner class DummyCheck : KotlinCheck {

        override fun initialize(initContext: InitContext) {
            initContext.register(KtNamedFunction::class.java, spyVisitor)
        }
    }

    @Test
    fun ensure_check_registration_works() {
        val inputFile = createInputFile("file1.kt", """
            fun main(args: Array<String>) {
                print (1 == 1);
            }
             """.trimIndent())
        context.fileSystem().add(inputFile)
        val dummyCheck = spyk(DummyCheck())
        KotlinSensor(checkFactory("S99999"), fileLinesContextFactory, NoSonarFilter(), language()).also { sensor ->
            sensor.checks.addAnnotatedChecks(dummyCheck)
            sensor.execute(context)
        }

        verify(exactly = 1) { dummyCheck.initialize(any()) }
        verify(exactly = 1) { spyVisitor(any(), any()) }
    }

    override fun repositoryKey(): String {
        return KotlinPlugin.KOTLIN_REPOSITORY_KEY
    }

    override fun language(): KotlinLanguage {
        return KotlinLanguage(MapSettings().asConfig())
    }
}
