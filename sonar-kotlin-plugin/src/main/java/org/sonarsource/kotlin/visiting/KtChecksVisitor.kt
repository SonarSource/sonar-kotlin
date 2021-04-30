package org.sonarsource.kotlin.visiting

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.sonar.api.batch.rule.Checks
import org.sonar.api.rule.RuleKey
import org.sonarsource.kotlin.api.CheckContext
import org.sonarsource.kotlin.api.Consumer
import org.sonarsource.kotlin.api.InitContext
import org.sonarsource.kotlin.api.KotlinCheck
import org.sonarsource.kotlin.converter.KotlinTree
import org.sonarsource.slang.api.Tree
import org.sonarsource.slang.plugin.InputFileContext
import org.sonarsource.slang.visitors.TreeVisitor

class KtChecksVisitor(val checks: Checks<KotlinCheck>) : TreeVisitor<InputFileContext>() {

    internal val consumers = mutableListOf<Consumer<PsiElement>>()

    init {
        // We need to convert the result of checks.ruleKey(it) to a non-nullable type here, as the method could theoretically return null
        // but in reality this never happens with a properly-configured plugin. If it does happen, we will already notice while testing.
        checks.all().forEach { it.initialize(ContextAdapter(checks.ruleKey(it)!!)) }
    }

    override fun scan(fileContext: InputFileContext, root: Tree?) {
        if (root is KotlinTree) {
            visit(fileContext, root.psiFile)
        }
    }

    private fun visit(fileContext: InputFileContext, psiElement: PsiElement) {
        consumers.forEach { it(fileContext, psiElement) }
        psiElement.children.forEach { visit(fileContext, it) }
    }

    internal fun <T : PsiElement> ktRegister(cls: Class<T>, visitor: Consumer<T>): TreeVisitor<InputFileContext> {
        consumers.add { fileContext, node ->
            if (cls.isAssignableFrom(node::class.java)) visitor(fileContext, cls.cast(node))
        }
        return this
    }

    inner class ContextAdapter(val ruleKey: RuleKey) : InitContext, CheckContext {

        lateinit var currentContext: InputFileContext

        override fun <T : PsiElement> register(cls: Class<T>, visitor: Consumer<T>) {
            ktRegister(cls) { fileContext, psiElement ->
                currentContext = fileContext
                visitor(fileContext, psiElement)
            }
        }
    }
}
