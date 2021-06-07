package checks;

class TooManyCasesCheckSample {

    fun example(c: Int) {
        when (c) { // Noncompliant {{Reduce the number of when branches from 3 to at most 2.}}
//      ^^^^
            1 -> {}
//            ^^<
            2 -> {}
//            ^^<
            else -> {}
//               ^^<
        }

        when (c) { // Compliant
            1 -> {}
        }
    }

}
