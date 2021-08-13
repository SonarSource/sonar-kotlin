package checks

abstract class TooLongLambdaCheckSample {

    constructor() {
        println()
        println()
    }

    fun example() {
        println()
        println()
    }

    fun compliant() {
        println()
    }

    fun lambda() {
        { // Noncompliant {{This lambda has 4 lines of code, which is greater than the 3 authorized. Split it into smaller functions.}}
//      ^
            println()
            println()
            println()
            println()
        }
    }

    fun multilineExpression() = println(
        """
        """
            .trimIndent())

    abstract fun abstractFun()

    fun String.extension() {
        println()
        println()
    }

    val x = {} // Compliant

    val y = {
        println()
        println()
    }
}
