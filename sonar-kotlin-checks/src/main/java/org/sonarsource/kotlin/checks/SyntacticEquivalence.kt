/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2025 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the Sonar Source-Available License Version 1, as published by SonarSource SA.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the Sonar Source-Available License for more details.
 *
 * You should have received a copy of the Sonar Source-Available License
 * along with this program; if not, see https://sonarsource.com/license/ssal/
 */
package org.sonarsource.kotlin.checks

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtElement

object SyntacticEquivalence {
    private fun areEquivalent(first: Iterator<PsiElement>, second: Iterator<PsiElement>): Boolean {
        val firstIterator = first.iterator()
        val secondIterator = second.iterator()
        while (firstIterator.hasNext() && secondIterator.hasNext()) {
            if (!areEquivalent(firstIterator.next(), secondIterator.next())) {
                return false
            }
        }
        return !firstIterator.hasNext() && !secondIterator.hasNext()
    }

    fun areEquivalent(first: PsiElement, second: PsiElement): Boolean {
        if (first === second) {
            return true
        }
        if (first.javaClass != second.javaClass) {
            return false
        }
        val leftChildrenIterator = first.children.asSequence().filter { it is KtElement }.iterator()
        val rightChildrenIterator = second.children.asSequence().filter { it is KtElement }.iterator()
        if (!leftChildrenIterator.hasNext() && !rightChildrenIterator.hasNext()) {
            return first.text == second.text
        }
        return areEquivalent(leftChildrenIterator, rightChildrenIterator)
    }

    fun findDuplicatedGroups(list: List<KtElement>): List<List<KtElement>> {
        return list.groupBy(::ComparableTree)
            .values.filter { group -> group.size > 1 }
    }
}

internal class ComparableTree(private val tree: KtElement) {
    private val hash = computeHash(tree)

    override fun equals(other: Any?): Boolean {
        if (other !is ComparableTree) {
            return false
        }
        return hash == other.hash && SyntacticEquivalence.areEquivalent(tree, other.tree)
    }

    override fun hashCode(): Int {
        return hash
    }

    companion object {
        private fun computeHash(tree: KtElement): Int {
            val children = tree.children.filterIsInstance<KtElement>()
            return if (children.isNullOrEmpty()) {
                tree.text.trim().hashCode()
            } else {
                children.contentHashCode()
            }
        }

        private fun List<KtElement>.contentHashCode(): Int {
            var hash = 7
            forEach {
                hash = 31 * hash + computeHash(it)
            }
            return hash
        }
    }
}
