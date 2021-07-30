package checks

abstract class UnusedFunctionParameterCheckSample {

    constructor(unused: String)

    private operator fun plus(unused: String): String = "" // Noncompliant {{Remove this unused function parameter "unused".}}
//                            ^^^^^^

    private fun unusedParameters(unused1: Int, used: Int, unused2: Int) = used // Noncompliant {{Remove these unused function parameters.}}
//                               ^^^^^^^                  ^^^^^^^<  {{Remove this unused method parameter unused2".}}
//                               ^^^^^^^@-1<      {{Remove this unused method parameter unused1".}}

    fun publicFun(unused: String) = Unit

    private external fun externalFun(unused: String)

    private fun String.extension(unused: String) { // Noncompliant
    }

    // TODO false-negatives - use underscore for unused lambda parameters and anonymous functions
    val lambda = { unused: Int, _: Int -> }
    val anonymousFunction = fun(unused: Int, _: Int) = Unit
}

fun backticks(`i`: Int) = i

fun backticks2(`i`: Int) = `i`

fun backticks3(`i 1`: Int) = `i 1`

fun backticks4(`i 1`: Int) = Unit // Noncompliant {{Remove this unused function parameter "i 1".}}

fun unusedParameterInTopLevelFun(unused: Int) {} // Noncompliant

fun usedParameterInTopLevelFun(used: Int) = used


fun main(args: Array<String>) {} // Noncompliant
fun main(a: Int) {} // Noncompliant
fun Main(a: Int) {} // Noncompliant
