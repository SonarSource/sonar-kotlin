package org.sonarsource.kotlin.converter.ast

fun function(
        input: String,
        isValid: Boolean = true,
        times: Int = 3
) {
}

fun main() {
    function("This is a String!", isValid = false, 5)
}

