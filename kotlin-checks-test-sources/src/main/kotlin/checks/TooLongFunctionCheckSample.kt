package checks

abstract class TooLongFunctionCheckSample {

    constructor() { // Noncompliant {{This function has 4 lines of code, which is greater than the 3 authorized. Split it into smaller functions.}}
        println()
        println()
    }

    // TODO false-negative
    init {
        println()
        println()
    }

    fun example() { // Noncompliant {{This function has 4 lines of code, which is greater than the 3 authorized. Split it into smaller functions.}}
//      ^^^^^^^
        println()
        println()
    }

    fun compliant() {
        println()
    }

    fun lambda() { // Noncompliant {{This function has 8 lines of code, which is greater than the 3 authorized. Split it into smaller functions.}}
        { // Noncompliant {{This function has 4 lines of code, which is greater than the 3 authorized. Split it into smaller functions.}}
//      ^
            println()
            println()
            println()
            println()
        }
    }

    fun multilineExpression() = println( // Noncompliant {{This function has 4 lines of code, which is greater than the 3 authorized. Split it into smaller functions.}}
        """
        """
            .trimIndent())

    abstract fun abstractFun()

    // TODO false-negative
    fun String.extension() {
        println()
        println()
    }

}
