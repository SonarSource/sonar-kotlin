package checks

class NestedMatchCheckSample {

    fun example(c: Int) {
        when (c) {
            1 -> when (c) { // Noncompliant {{Refactor the code to eliminate this nested "when".}}
                2 -> {}
            }
        }

        when (c) { // Compliant
            1 -> {}
        }
    }

}
