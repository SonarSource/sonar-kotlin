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

        @Suppress("some other rule")
        println("this is a duplicate print") // Noncompliant
        @Suppress("some other rule")
        println("this is a duplicate print")
        @Suppress("some other rule")
        println("this is a duplicate print")

        // Noncompliant@+4
        println(
            """
                not this is the one
                ${"but this here!"}
        """.trimIndent()
        )
//@-2             ^^^^^^^^^^^^^^^^
        println(
            """
                not this is the one
                ${"but this here!"}
        """.trimIndent()
        )
//@-2             ^^^^^^^^^^^^^^^^<
        println(
            """
                not this is the one
                ${"but this here!"}
        """.trimIndent()
        )
//@-2             ^^^^^^^^^^^^^^^^<
    }

    @Deprecated("Conversion of Char to Number is deprecated. Use Char.code property instead.", ReplaceWith("this.code.toShort()"))
    fun d1() {
    }

    @Deprecated("Conversion of Char to Number is deprecated. Use Char.code property instead.", ReplaceWith("this.code.toShort()"))
    fun d2() {
    }

    @Deprecated("Conversion of Char to Number is deprecated. Use Char.code property instead.", ReplaceWith("this.code.toShort()"))
    fun d3() {
    }
}
