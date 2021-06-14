package checks

class FunctionCognitiveComplexityCheckSample {

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

    // TODO false-negative
    fun String.extension() {
        fun() {
            x && y || x && y || x && y
        }
    }

}
