package org.sonarsource.kotlin.visiting

import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.junit.jupiter.api.Test
import org.sonar.api.batch.rule.Checks
import org.sonarsource.kotlin.api.KotlinCheck
import org.sonarsource.slang.api.Tree
import org.sonarsource.slang.impl.BaseTreeImpl
import org.sonarsource.slang.plugin.InputFileContext

class KtChecksVisitorTest {
    @Test
    fun `invalid node type passed to scan`() {
        val checks = mockk<Checks<KotlinCheck>> {
            every { all() } returns emptyList()
        }

        val testedVisitor = spyk(KtChecksVisitor(checks), recordPrivateCalls = true)

        testedVisitor.scan(mockk(), object : BaseTreeImpl(mockk()) {
            override fun children() = emptyList<Tree>()
        })

        verify(exactly = 0) { testedVisitor["visit"](ofType<InputFileContext>(), ofType<PsiElement>()) }
    }
}
