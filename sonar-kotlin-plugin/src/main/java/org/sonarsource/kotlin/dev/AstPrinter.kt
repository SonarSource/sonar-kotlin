package org.sonarsource.kotlin.dev

import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import java.io.File
import java.nio.file.Path
import kotlin.io.path.writeText

object AstPrinter {
    private const val INDENT = "  ";

    fun dotPrint(node: PsiElement): String = dotPrint(DotNode.of(node))
    fun dotPrint(node: PsiElement, outputFile: Path) = dotPrint(DotNode.of(node), outputFile)

    fun dotPrint(node: DotNode) = toDotString(node)
    fun dotPrint(node: DotNode, outputFile: Path) = outputFile.writeText(dotPrint(node))

    fun txtPrint(node: PsiElement): String = txtPrint(DotNode.of(node))
    fun txtPrint(node: PsiElement, outputFile: Path) = txtPrint(DotNode.of(node), outputFile)

    fun txtPrint(node: DotNode) = toTxtString(node)
    fun txtPrint(node: DotNode, outputFile: Path) = outputFile.writeText(txtPrint(node))

    private fun toTxtString(node: DotNode, indentLevel: Int = 0): String =
        "${INDENT.repeat(indentLevel)}${node.type}: ${node.txtLabel()}\n" +
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

data class DotNode(val title: String, val text: String, val type: String, val children: List<DotNode>) {
    companion object {
        var nextId = 0

        fun of(original: PsiElement): DotNode {
            val title = when (original) {
                is KtFile -> original.name.substringAfterLast(File.separatorChar)
                is LeafPsiElement -> original.elementType.toString()
                else -> ""
            }

            val text = with(original.text.trim()) {
                if (length <= 50) this
                else "${substring(0, 20)}...${substring(length - 20)}"
            }.replace("\n", "")

            return DotNode(title, text, original::class.java.simpleName, original.allChildren.map { of(it) }.toList())
        }
    }

    val id = nextId++

    fun htmlLabel() = (if (title.isNotBlank()) "<i>${title.escapeHtml()}</i><br/>" else "") + text.escapeHtml()

    fun txtLabel() = "[$title] $text"
}

private fun String.escapeHtml() = this
    .replace("<", "&lt;")
    .replace(">", "&gt;")
