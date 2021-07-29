package checks

fun List<String>.print() = this.forEach { println(it) }

fun Map<String, String>.print() = this.forEach { (k, v) -> println() }  // Noncompliant {{Use "_" instead of these unused lambda parameters.}}
//                                                ^  ^<  {{Use "_" instead of this unused lambda parameter "v".}}
//                                                ^@-1<      {{Use "_" instead of this unused lambda parameter "k".}}

abstract class UnusedFunctionParameterCheckSample {

    constructor(unused: String)

    private operator fun plus(unused: String): String = "" // Noncompliant {{Remove this unused function parameter "unused".}}
//                            ^^^^^^

    private fun unusedParameters(unused1: Int, used: Int, unused2: Int) = used // Noncompliant {{Remove these unused function parameters.}}
//                               ^^^^^^^                  ^^^^^^^<  {{Remove this unused function parameter "unused2".}}
//                               ^^^^^^^@-1<      {{Remove this unused function parameter "unused1".}}

    fun publicFun(unused: String) = Unit

    private external fun externalFun(unused: String)

    private fun String.extension(unused: String) { // Noncompliant
    }

    val lambda = { unused: Int, _: Int -> }   // Noncompliant {{Use "_" instead of this unused lambda parameter "unused".}}
//                 ^^^^^^


    val anonymousFunction = fun(unused: Int, _: Int) = Unit  // Noncompliant {{Use "_" instead of this unused function parameter "unused".}}
//                              ^^^^^^
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
