package org.sonarsource.kotlin.converter.ast

fun testBreakAndContinue(ints: List<Int>) {
    for (i in ints) {
        when (i) {
            in 2..5 -> continue
            39 -> break
            else -> println(i)
        }
    }
}