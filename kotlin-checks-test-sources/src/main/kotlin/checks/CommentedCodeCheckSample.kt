/* No detection of commented-out code in header
 * if (true) println()
 */
package checks

/**
 * No detection of KDoc
 * if (true) println()
 */
class CommentedCodeCheckSample {
    // Noncompliant@+2 {{Remove this commented out code.}}

    // if (true) println()
    // if (true) println()

    // Noncompliant@+2 {{Remove this commented out code.}}

    /*
    if (true) println()
     */

    /** if (true) println() */

    /**
     * if (true) println()
     */

    /** see [checks.CommentedCodeCheckSample] */

    /**
     * see [checks.CommentedCodeCheckSample]
     */

    // Compliant, as formatted in backticks
    // `if (true) println()`

    // Noncompliant@+2

//            val (valid, invalid) = conns.filter { it.isDone }.partition { it.get() == "Deadlock ?" }
//
//            println("Completed: ${valid.size} valid, ${invalid.size} invalid of ${conns.size} total [attempts $attempts]")

    // Noncompliant@+2

//    println("This")
//    println("is")
//    println("commented-out")
//    println("code")
//    println("and")
//    println("detected")

    // Noncompliant@+2

    //if (true) println()

    // Compliant@+2 tihs is actually parsed as code - many nested infix notation expressions. But it is - obviously - not code.

    // only unlocked states only soft locked states only those soft locked states specified by lock id(s) all unlocked
    // states plus those soft locked states specified by lock id(s)
}
