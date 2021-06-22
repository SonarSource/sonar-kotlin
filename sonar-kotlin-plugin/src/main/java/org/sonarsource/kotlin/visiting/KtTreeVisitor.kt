package org.sonarsource.kotlin.visiting

import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.KtVisitorVoid

abstract class KtTreeVisitor : KtVisitorVoid() {
    fun visitTree(node: KtElement) {
        node.accept(this)
        node.children.forEach { if (it is KtElement) visitTree(it) }
    }
}
