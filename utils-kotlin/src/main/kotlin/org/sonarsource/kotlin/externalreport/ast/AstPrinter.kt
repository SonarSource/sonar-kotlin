package org.sonarsource.kotlin.externalreport.ast

import org.sonarsource.kotlin.converter.KotlinConverter
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

    val converter = KotlinConverter(emptyList())
    val inputFileName = inputFile.fileName
    val kotlinTree by lazy { converter.parse(inputFile.readText(), inputFileName.toString()) as KotlinTree }

    when (mode) {
        "dot" ->
            if (args.size > 2) AstPrinter.dotPrint(kotlinTree.psiFile, Path.of(args[2]))
            else println(AstPrinter.dotPrint(kotlinTree.psiFile))
        "txt" ->
            if (args.size > 2) AstPrinter.txtPrint(kotlinTree.psiFile, Path.of(args[2]))
            else println(AstPrinter.txtPrint(kotlinTree.psiFile))
        else -> exitWithUsageInfoAndError()
    }
}

private const val USAGE_MSG = """Usage: main(<dot|txt>, <inputFile>[, outputFile])""""
private fun exitWithUsageInfoAndError() {
    System.err.println(USAGE_MSG)
    throw IllegalArgumentException(USAGE_MSG)
}
