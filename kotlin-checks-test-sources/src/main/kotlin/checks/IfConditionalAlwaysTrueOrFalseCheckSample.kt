package checks

class IfConditionalAlwaysTrueOrFalseCheckSample {

    fun test(c: Boolean) {
        if (true) println() // Noncompliant {{Remove this useless "if" statement.}}
//          ^^^^
        if (false) println() // Noncompliant

        if (!true) println() // Noncompliant
        if (!false) println() // Noncompliant

        if (c) println()
        if (!c) println()
        if (c == c) println()

        if (c && false) println() // Noncompliant
        if (c && true) println()

        if (c || true) println() // Noncompliant
        if (c || false) println()

        if ("".equals(c) && false) println()
        if (false && "".equals(c)) println()
    }

}
