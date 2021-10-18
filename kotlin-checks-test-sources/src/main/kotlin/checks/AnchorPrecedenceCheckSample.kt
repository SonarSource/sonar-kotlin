package checks

class AnchorPrecedenceCheckSample {
    private fun noncompliant() {
        Regex("^a|b|c\$") // Noncompliant {{Group parts of the regex together to make the intended operator precedence explicit.}}
//             ^^^^^^^^
        Regex("^a|b|c$") // Noncompliant
        Regex("""^a|b|c${'$'}""") // Compliant FN due to string interpolation
        Regex("""^a|b|c${"$"}""") // Compliant FN due to string interpolation
        Regex("""^a|b|c$""") // Noncompliant

        "^a|b|c\$".toRegex() // Noncompliant
//       ^^^^^^^^  ^^^^^^^<
        "^a|b|c$".toRegex() // Noncompliant
        """^a|b|c${'$'}""".toRegex() // Compliant FN due to string interpolation
        """^a|b|c${"$"}""".toRegex() // Compliant FN due to string interpolation
        """^a|b|c$""".toRegex() // Noncompliant

        Regex("^a|b|cd") // Noncompliant
        Regex("""^a|b|cd""") // Noncompliant
        Regex("(?i)^a|b|cd") // Noncompliant
        Regex("""(?i)^a|b|cd""") // Noncompliant
//               ^^^^^^^^^^^
        Regex("(?i:^a|b|cd)") // Noncompliant
        Regex("""(?i:^a|b|cd)""") // Noncompliant
        Regex("a|b|c$") // Noncompliant
        Regex("""a|b|c$""") // Noncompliant
        Regex("\\Aa|b|c\\Z") // Noncompliant
        Regex("""\Aa|b|c\Z""") // Noncompliant
        Regex("\\Aa|b|c\\z") // Noncompliant
        Regex("""\Aa|b|c\z""") // Noncompliant
        Regex("testing(this)?(a|b|c$)") // Noncompliant
//                            ^^^^^^
    }

    private fun compliant() {
        Regex("^(?:a|b|c)\$")
        Regex("^(?:a|b|c)$")
        Regex("""^(?:a|b|c)${'$'}""")
        Regex("""^(?:a|b|c)$""")

        Regex("^a\$|^b\$|^c\$")
        Regex("^a$|^b$|^c$")
        Regex("""^a${'$'}|^b${'$'}|^c${'$'}""")
        Regex("""^a$|^b$|^c$""")

        Regex("(?:^a)|b|(?:c\$)")
        Regex("(?:^a)|b|(?:c$)")
        Regex("""(?:^a)|b|(?:c${'$'})""")
        Regex("(?:^a)|b|(?:c$)")

        Regex("^abc$")
        Regex("a|b|c")
        Regex("^a$|b|c")
        Regex("a|b|^c$")
        Regex("^a|^b$|c$")
        Regex("^a|^b|c$")
        Regex("^a|b$|c$")
        // Only beginning and end of line/input boundaries are considered - not word boundaries
        Regex("\\ba|b|c\\b")
        Regex("\\ba\\b|\\bb\\b|\\bc\\b")
        // If multiple alternatives are anchored, but not all, that's more likely to be intentional than if only the first
        // one were anchored, so we won't report an issue for the following line:
        Regex("^a|^b|c")
        Regex("aa|bb|cc")
        Regex("^")
        Regex("^[abc]$")
        Regex("|")
        Regex("[")
        Regex("(?i:^)a|b|c")
    }
}
