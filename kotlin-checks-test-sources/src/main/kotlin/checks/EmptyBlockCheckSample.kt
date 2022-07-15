package checks

class EmptyBlockCheckSample {

    fun example(c: Boolean) {
        if (c) {} // Noncompliant {{Either remove or fill this block of code.}}

        if (c) {
            // Compliant - contains comment
        }

        if (c) {
            /* Compliant - contains comment */
        }

        if (c) {
            /** Compliant - contains KDoc */
        }

        /* The following is commented out since we upgraded to 1.7, as it does not compile anymore (non-exhaustive when statements have
         * become a compile-time error with 1.7). If we introduce test source parsing with different Kotlin versions at some point, this
         * could be re-enabled for a Kotlin version <1.7.

        when (c) {} // Noncompliant

        when (c) {
            // Compliant - contains comment
        }*/

        when (c) {
            true -> {} // Noncompliant
            else -> {
                // Compliant - contains comment
            }
        }

        while (c) {} // Compliant - exception to the rule
    }

    fun emptyFunctionIsCompliant() {
    }

}
