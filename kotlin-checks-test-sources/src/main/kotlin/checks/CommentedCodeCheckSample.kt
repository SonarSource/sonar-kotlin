package checks

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
}
