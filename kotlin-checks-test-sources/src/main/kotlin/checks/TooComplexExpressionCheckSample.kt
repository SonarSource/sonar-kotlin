package checks

class TooComplexExpressionCheckSample {
    fun test(
        a: Boolean,
        b: Boolean,
        c: Boolean,
        d: Boolean,
        e: Boolean,
    ) {
        println((a && b) || !c || (d xor e))
        println((a && b) || !c || (d && e)) // Noncompliant {{Reduce the number of conditional operators (4) used in the expression (maximum allowed 3).}}
//              ^^^^^^^^^^^^^^^^^^^^^^^^^^

        println(a || // Noncompliant {{Reduce the number of conditional operators (5) used in the expression (maximum allowed 3).}}
            (a || b || c || d || e)) // Noncompliant {{Reduce the number of conditional operators (4) used in the expression (maximum allowed 3).}}
    }
}
