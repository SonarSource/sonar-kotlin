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

        when (c) {} // Noncompliant

        when (c) {
            // Compliant - contains comment
        }

        when (c) {
            true -> {} // Noncompliant
        }

        while (c) {} // Compliant - exception to the rule
    }

    fun emptyFunctionIsCompliant() {
    }

}
