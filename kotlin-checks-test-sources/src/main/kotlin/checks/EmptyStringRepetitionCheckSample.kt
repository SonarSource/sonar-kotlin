package checks

class EmptyStringRepetitionCheckSample {
    fun noncompliant(input: String) {
        Regex("(?:)*") // Noncompliant {{Rework this part of the regex to not match the empty string.}}
//             ^^^^
        Regex("(?:)?") // Noncompliant
        Regex("(?:)+") // Noncompliant
        Regex("()*") // Noncompliant
        Regex("()?") // Noncompliant
        Regex("()+") // Noncompliant
        Regex("xyz|(?:)*") // Noncompliant
//                 ^^^^
        Regex("(?:|x)*") // Noncompliant
        Regex("(?:x|)*") // Noncompliant
        Regex("(?:x|y*)*") // Noncompliant
        Regex("(?:x*|y*)*") // Noncompliant
        Regex("(?:x?|y*)*") // Noncompliant
        Regex("(?:x*)*") // Noncompliant
        Regex("(?:x?)*") // Noncompliant
        Regex("(?:x*)?") // Noncompliant
        Regex("(?:x?)?") // Noncompliant
        Regex("(?:x*)+") // Noncompliant
        Regex("(?:x?)+") // Noncompliant
        Regex("(x*)*") // Noncompliant
        Regex("((x*))*") // Noncompliant
        Regex("(?:x*y*)*") // Noncompliant
        Regex("(?:())*") // Noncompliant
        Regex("(?:(?:))*") // Noncompliant
        Regex("((?i))*") // Noncompliant
        Regex("(())*") // Noncompliant
        Regex("(()x*)*") // Noncompliant
        Regex("(()|x)*") // Noncompliant
        Regex("($)*") // Noncompliant
        Regex("(\\b)*") // Noncompliant
        Regex("((?!x))*") // Noncompliant
    }

    fun compliant(input: String) {
        Regex("x*|")
        Regex("x*|")
        Regex("x*")
        Regex("x?")
        Regex("(?:x|y)*")
        Regex("(?:x+)+")
        Regex("(?:x+)*")
        Regex("(?:x+)?")
        Regex("((x+))*")
    }

    fun no_duplications(input: String) {
        // Noncompliant@+1 2
        val regex = "(?:)*"
//                   ^^^^
        Regex(regex)
//      ^^^^^<
        Regex(regex)

        // Noncompliant@+1 2
        val regex2_1 = "(?:"
//                      ^^^
        val regex2_2 = ")*"
//                      ^<
        Regex(regex2_1 + regex2_2)
//      ^^^^^<
        Regex(regex2_1 + regex2_2)

        val regex3_1 = "(?:"
        val regex3_2 = ")*"
        Regex(regex3_1 + "x|y" + regex3_2)
    }
}
