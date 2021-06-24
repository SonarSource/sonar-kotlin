package org.sonarsource.kotlin.visiting

import org.sonarsource.kotlin.api.InputFileContext
import org.sonarsource.kotlin.converter.KotlinTree
import org.sonarsource.kotlin.plugin.KotlinFileContext

abstract class KotlinFileVisitor {
    fun scan(fileContext: InputFileContext, root: KotlinTree) {
        visit(KotlinFileContext(fileContext, root.psiFile, root.bindingContext))
    }

    abstract fun visit(kotlinFileContext: KotlinFileContext)
}
