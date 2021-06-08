package checks

class CollapsibleIfStatementsCheck {
    fun test(c: Boolean) {
        if (c) // Noncompliant {{Merge this "if" statement with the nested one.}}
//      ^^
            if (c)
//          ^^<
                println()

        if (c) { // Noncompliant
//      ^^
            if (c)
//          ^^<
                println()
        }

        if (c) {
            if (c)
                println()
            println()
        }

        if (c)
            if (c)
                println()
            else
                println()
    }
}
