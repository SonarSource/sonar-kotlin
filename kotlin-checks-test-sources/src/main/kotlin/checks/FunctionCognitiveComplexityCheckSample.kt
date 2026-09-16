package checks

class FunctionCognitiveComplexityCheckSample {
    annotation class Composable

    val x: Boolean = false
    val y: Boolean = false
    val z: Boolean = false

    fun ko() { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 5 to the 4 allowed.}} [[effortToFix=1]]
//      ^^
        if (x) {
//      ^^< {{+1}}
            if (y) {
//          ^^< {{+2 (incl 1 for nesting)}}
                println()
            }
            if (z) {
//          ^^< {{+2 (incl 1 for nesting)}}
                println()
            }
        }
    }

    fun ok() {
        if (x) {
            if (y) {
                println()
            }
        }
    }

    fun logical_operators() { // Noncompliant
//      ^^^^^^^^^^^^^^^^^
        if (x
//      ^^<
            && y && z
//          ^^<
            || y || z
//          ^^<
            && x
//          ^^<
            || y || z) {
//          ^^<
            println()
        }
    }

    fun nesting_anonymous() { // Noncompliant
        fun() {
            x && y || x && y || x && y
        }
    }

    fun forLoop() { // Noncompliant
        if (x) {
            for (i in 0..1)
                if (x && y)
                    break
        }
    }

    fun whileLoop() { // Noncompliant
        if (x) {
            outer@ while (x || y) {
                while (x && y)
                    continue@outer
            }
        }
    }

    fun whenClause() { // Noncompliant
        when (x) {
            true -> if (x || y) {
                if (z) 0
            } else 1
            false -> null
        }
    }

    fun catchClause() { // Noncompliant
        try {
        } catch(e: Exception) {
            if (x)
                if (y)
                    if (z)
                        return
        }
    }

    fun elseWithIf() { // Noncompliant
        if (x) 0
        else if (y) 1
        else if (z) 2
        else if (!x && !z) 3
        else if (y || z) 4
    }

    fun ternary() { // Noncompliant
        if (x) 0 else 1
        if (y) 1 else 2
        if (z) 2 else 3
        if (x) 3 else 4
        if (y) 4 else 5
    }

    fun non_ternary() { // Noncompliant
        if (x) {
            0
        } else {
            1
        }
        if (y) 1
        else {
            2
        }
        if (z) {
            2
        } else 3
    }

    fun lambdas() { // Noncompliant
        x.let {
            it.apply {
                it.also {
                    it.run {
                        with(it) {
                            if (x)
                                return
                        }
                    }
                }
            }
        }
    }

    fun innerClass() { // Compliant
        x.let {
            it.apply {
                it.also {
                    it.run {
                        with(it) {
                            class Clazz {
                                init {
                                    if(x) println()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun String.extension() { // Noncompliant
        fun() {
            x && y || x && y || x && y
        }
    }

    @Composable
    fun composableCompliant() { // Compliant - @Composable threshold is 45; complexity is 5 which exceeds the regular threshold of 4 but not the composable one
        if (x) { // +1
            if (y) { // +2
                println()
            }
            if (z) { // +2
                println()
            }
        }
    }

    @Composable
    fun composableNoncompliant() { // Noncompliant {{Refactor this method to reduce its Cognitive Complexity from 46 to the 45 allowed.}} [[effortToFix=1]]
        if (x) { // +1
            if (x) { // +2
                if (x) { // +3
                    if (x) { // +4
                        if (x) { // +5
                            if (x) { // +6
                                if (x) { // +7
                                    if (x) { // +8
                                        if (x) { // +9
                                            println()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (x) println() // +1
    }

}
