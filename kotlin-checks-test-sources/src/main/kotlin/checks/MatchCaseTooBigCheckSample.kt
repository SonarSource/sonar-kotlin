package checks

class MatchCaseTooBigCheckSample {

    fun example(c: Int) {
        when (c) {
            0 -> {
                println()
            }
        }

        when (c) {
            0 -> { // Noncompliant {{Reduce this case clause number of lines from 17 to at most 15, for example by extracting code into methods.}}
                println()
                println()
                println()
                println()
                println()
                println()
                println()
                println()
                println()
                println()
                println()
                println()
                println()
                println()
                println()
            }
        }
    }

}
