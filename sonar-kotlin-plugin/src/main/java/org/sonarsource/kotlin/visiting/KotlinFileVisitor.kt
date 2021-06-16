package org.sonarsource.kotlin.visiting

import org.sonarsource.kotlin.converter.KotlinTree
import org.sonarsource.kotlin.plugin.KotlinFileContext
import org.sonarsource.slang.api.Tree
import org.sonarsource.slang.plugin.InputFileContext
import org.sonarsource.slang.visitors.TreeVisitor

abstract class KotlinFileVisitor : TreeVisitor<InputFileContext>() {
    override fun scan(fileContext: InputFileContext, root: Tree?) {
        if (root is KotlinTree) {
            visit(KotlinFileContext(fileContext, root.psiFile, root.bindingContext))
        }
    }

    abstract fun visit(kotlinFileContext: KotlinFileContext)
}
