package checks

class CodeAfterJumpCheckSample {

    fun testReturn(): Int {
        return 42 // Noncompliant {{Refactor this piece of code to not have any dead code after this "return".}}
//      ^^^^^^^^^
        42
    }

    fun lastReturn(): Int {
        42
        return 42
    }

    fun testBreak(cond: Boolean) {
        while (cond) {
            break // Noncompliant {{Refactor this piece of code to not have any dead code after this "break".}}
//          ^^^^^
            42
            42
        }
    }

    fun testContinue(cond: Boolean) {
        while (cond) {
            continue  // Noncompliant {{Refactor this piece of code to not have any dead code after this "continue".}}
//          ^^^^^^^^
            42
        }
    }

    fun testContinueWithLabel(cond: Boolean) {
        myLabel@ while (cond) {
            continue@myLabel   // Noncompliant {{Refactor this piece of code to not have any dead code after this "continue".}}
//          ^^^^^^^^^^^^^^^^
            42
        }
    }

    fun empty() {}

    // TODO false-negative
    fun fnRequiresCfg(cond: Boolean) {
        if (cond) {
            return
        } else {
            return
        }
        42
    }

    fun testThrow() {
        throw Throwable() // Noncompliant {{Refactor this piece of code to not have any dead code after this "throw".}}
        42
    }

    fun testThrow2() {
        throw Throwable() // Noncompliant {{Refactor this piece of code to not have any dead code after this "throw".}}
        throw Throwable() // Noncompliant
        42
    }

}
