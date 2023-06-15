package org.sonarsource.kotlin.converter.ast

fun highOrderFunction(func: () -> Unit) { }
fun helloString(): String = "Hello, World!"

fun main() {
    highOrderFunction { helloString() } // this was the only way to do it  before 1.4
    highOrderFunction(::helloString) // starting from 1.4, this also works
}