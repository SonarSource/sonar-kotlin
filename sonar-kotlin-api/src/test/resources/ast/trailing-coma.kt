package org.sonarsource.kotlin.converter.ast

class Trailing(var a: List<String>,) {
    fun b(a: String,) {
        this.a = listOf(
                a,
                "",
        )
    }
}
