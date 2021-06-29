package org.sonarsource.kotlin.externalreport.ast

import org.sonarsource.kotlin.converter.Environment
import org.sonarsource.kotlin.converter.KotlinTree
import org.sonarsource.kotlin.dev.AstPrinter
import java.nio.file.Path
import kotlin.io.path.readText

fun main(vararg args: String) {
    if (args.size < 2) {
        exitWithUsageInfoAndError()
    }

    val mode = args[0].lowercase()
    val inputFile = Path.of(args[1])
    val environment = Environment(emptyList())

    val kotlinTree by lazy { KotlinTree.of(inputFile.readText(), environment) }

    when (mode) {
        "dot" ->
            if (args.size > 2) AstPrinter.dotPrint(kotlinTree.psiFile, Path.of(args[2]))
            else println(AstPrinter.dotPrint(kotlinTree.psiFile))
        "txt" ->
            if (args.size > 2) AstPrinter.txtPrint(kotlinTree.psiFile, Path.of(args[2]), kotlinTree.document)
            else println(AstPrinter.txtPrint(kotlinTree.psiFile, kotlinTree.document))
        else -> exitWithUsageInfoAndError()
    }
}

private const val USAGE_MSG = """Usage: main(<dot|txt>, <inputFile>[, outputFile])""""
private fun exitWithUsageInfoAndError() {
    System.err.println(USAGE_MSG)
    throw IllegalArgumentException(USAGE_MSG)
}
