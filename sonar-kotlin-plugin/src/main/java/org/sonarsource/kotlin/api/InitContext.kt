package org.sonarsource.kotlin.api

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.sonarsource.slang.plugin.InputFileContext

interface InitContext {
    fun <T : PsiElement> register(cls: Class<T>, visitor: Consumer<T>)
}

typealias Consumer<T> = (InputFileContext, T) -> Unit

