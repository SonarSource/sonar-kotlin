package checks

class StringLiteralDuplicatedCheckSample {
    fun test() {
        // Noncompliant@+1
        """abcdefg
            |cdasv
        """.trimMargin()

        """abcdefg
            |cdasv
        """.trimMargin()

        """abcdefg
            |cdasv
        """.trimMargin()

        "appears twice"
        "appears twice" // Compliant - literal only appears twice

        "single_word"
        "single_word"
        "single_word"  // Compliant - single word

        "\\xff"
        "\\xff"
        "\\xff"

        var x = ""
        "with interpolation $x"
        "with interpolation $x"
        "with interpolation $x" // Compliant - contains interpolation

        // Noncompliant@+1
        @Suppress("""kotlin:S1192""")
        println()
        @Suppress("""kotlin:S1192""")
        println()
        @Suppress("""kotlin:S1192""")
        println()
    }
}
