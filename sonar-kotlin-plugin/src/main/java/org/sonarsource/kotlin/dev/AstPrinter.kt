/*
 * SonarSource Kotlin
 * Copyright (C) 2018-2023 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonarsource.kotlin.dev

import org.jetbrains.kotlin.com.intellij.openapi.editor.Document
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import java.io.File
import java.nio.file.Path
import kotlin.io.path.writeText

object AstPrinter {
    private const val INDENT = "  "

    fun dotPrint(node: PsiElement): String = dotPrint(DotNode.of(node, null))
    fun dotPrint(node: PsiElement, outputFile: Path) = dotPrint(DotNode.of(node, null), outputFile)

    fun dotPrint(node: DotNode) = toDotString(node)
    fun dotPrint(node: DotNode, outputFile: Path) = outputFile.writeText(dotPrint(node))

    fun txtPrint(node: PsiElement, document: Document? = null): String = txtPrint(DotNode.of(node, document))
    fun txtPrint(node: PsiElement, outputFile: Path, document: Document? = null) = txtPrint(DotNode.of(node, document), outputFile)

    fun txtPrint(node: DotNode) = toTxtString(node)
    fun txtPrint(node: DotNode, outputFile: Path) = outputFile.writeText(txtPrint(node))

    private fun toTxtString(node: DotNode, indentLevel: Int = 0): String =
        "${INDENT.repeat(indentLevel)}${node.type} ${node.range.prettyString()}: ${node.txtLabel()}\n" +
            node.children.joinToString("") { toTxtString(it, indentLevel + 1) }

    private fun toDotString(node: DotNode): String {
        return "digraph {\n${INDENT}graph [rankdir = LR]\n${dotPrintAllNodes(node)}\n}"
    }

    private fun dotPrintAllNodes(node: DotNode): String {
        val (nodes, edges) = collectAllNodesAndEdges(node)
        return nodes.joinToString("\n") { """$INDENT${it.id} [shape=box label=<<b>${it.type.escapeHtml()}</b><br/>${it.htmlLabel()}>]""" } + "\n" +
            edges.joinToString("\n") { (from, to) -> "$INDENT${from.id} -> ${to.id}" }
    }

    private fun collectAllNodesAndEdges(node: DotNode): Pair<List<DotNode>, List<Pair<DotNode, DotNode>>> {
        val localEdges = mutableListOf<Pair<DotNode, DotNode>>()
        val (nodes, edges) = node.children.map {
            localEdges.add(node to it)
            collectAllNodesAndEdges(it)
        }.foldRight(emptyList<DotNode>() to emptyList<Pair<DotNode, DotNode>>()) { x, acc ->
            (x.first + acc.first) to (x.second + acc.second)
        }

        return (listOf(node) + nodes) to (localEdges + edges)
    }


}

data class DotNode(val title: String, val text: String, val type: String, val children: List<DotNode>, val range: TextRange?) {
    companion object {
        var nextId = 0

        fun of(original: PsiElement, document: Document?): DotNode {
            val title = when (original) {
                is KtFile -> original.name.substringAfterLast(File.separatorChar)
                is LeafPsiElement -> original.elementType.toString()
                else -> ""
            }

            val text = with(original.text.trim()) {
                if (length <= 65) this
                else "${substring(0, 30)} … ${substring(length - 30)}"
            }.replace("\n", "")

            val range = document?.let {
                val start = textPointerAtOffset(it, original.startOffset)
                val end = textPointerAtOffset(it, original.endOffset)
                TextRange(start, end)
            }

            return DotNode(title, text, original::class.java.simpleName, original.allChildren.map { of(it, document) }.toList(), range)
        }
    }

    val id = nextId++

    fun htmlLabel() = (if (title.isNotBlank()) "<i>${title.escapeHtml()}</i><br/>" else "") + text.escapeHtml()

    fun txtLabel() = "[$title] $text"
}

private fun String.escapeHtml() = this
    .replace("<", "&lt;")
    .replace(">", "&gt;")

private fun TextRange?.prettyString() = this?.run {
    "${start.line}:${start.lineOffset} … ${end.line}:${end.lineOffset}"
} ?: "?-?"

fun textPointerAtOffset(psiDocument: Document, startOffset: Int): TextPointer {
    val startLineNumber = psiDocument.getLineNumber(startOffset)
    val startLineNumberOffset = psiDocument.getLineStartOffset(startLineNumber)
    val startLineOffset = startOffset - startLineNumberOffset

    return TextPointer(startLineNumber + 1, startLineOffset)
}

data class TextPointer(val line: Int, val lineOffset: Int)
data class TextRange(val start: TextPointer, val end: TextPointer)
