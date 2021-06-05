package checks

class OneStatementPerLineCheckSample {

    fun test() {
        println()

        println(); println(); println() // Noncompliant {{Reformat the code to have only one statement per line.}}
//                 ^^^^^^^^^  ^^^^^^^^^<
    }

}
