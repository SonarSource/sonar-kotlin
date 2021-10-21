package checks

class ReluctantQuantifierCheckSample {
    fun noncompliant() {
        Regex("<.+?>") // Noncompliant {{Replace this use of a reluctant quantifier with "[^>]++".}}
//              ^^^
        Regex("<\\S+?>") // Noncompliant {{Replace this use of a reluctant quantifier with "[^>\\s]++".}}
//              ^^^^^
        Regex("<\\D+?>") // Noncompliant {{Replace this use of a reluctant quantifier with "[^>\\d]++".}}
//              ^^^^^
        Regex("<\\W+?>") // Noncompliant {{Replace this use of a reluctant quantifier with "[^>\\w]++".}}
//              ^^^^^
        Regex("<.{2,5}?>") // Noncompliant {{Replace this use of a reluctant quantifier with "[^>]{2,5}+".}}
//              ^^^^^^^
        Regex("<\\S{2,5}?>") // Noncompliant {{Replace this use of a reluctant quantifier with "[^>\\s]{2,5}+".}}
//              ^^^^^^^^^
        Regex("<\\D{2,5}?>") // Noncompliant {{Replace this use of a reluctant quantifier with "[^>\\d]{2,5}+".}}
//              ^^^^^^^^^
        Regex("<\\W{2,5}?>") // Noncompliant {{Replace this use of a reluctant quantifier with "[^>\\w]{2,5}+".}}
//              ^^^^^^^^^
        Regex("<.{2,}?>") // Noncompliant {{Replace this use of a reluctant quantifier with "[^>]{2,}+".}}
//              ^^^^^^
        Regex("\".*?\"") // Noncompliant {{Replace this use of a reluctant quantifier with "[^\"]*+".}}
//               ^^^
        Regex(".*?\\w") // Noncompliant {{Replace this use of a reluctant quantifier with "\\W*+".}}
//             ^^^
        Regex(".*?\\W") // Noncompliant {{Replace this use of a reluctant quantifier with "\\w*+".}}
//             ^^^
        Regex(".*?\\p{L}") // Noncompliant {{Replace this use of a reluctant quantifier with "\\P{L}*+".}}
//             ^^^
        Regex(".*?\\P{L}") // Noncompliant {{Replace this use of a reluctant quantifier with "\\p{L}*+".}}
//             ^^^
        Regex("\\[.*?\\]") // Noncompliant {{Replace this use of a reluctant quantifier with "[^\\]]*+".}}
//                ^^^
        Regex(".+?[abc]") // Noncompliant {{Replace this use of a reluctant quantifier with "[^abc]++".}}
//             ^^^
        Regex("(?-U:\\s)*?\\S")
        Regex("(?U:\\s)*?\\S") // Noncompliant {{Replace this use of a reluctant quantifier with "[\\s\\S]*+".}}
//             ^^^^^^^^^^
        Regex("(?U:a|\\s)*?\\S")
        Regex("\\S*?\\s")
        Regex("\\S*?(?-U:\\s)")
        Regex("\\S*?(?U:\\s)") // Noncompliant {{Replace this use of a reluctant quantifier with "[\\S\\s]*+".}}
//             ^^^^^
        Regex("\\S*?(?U)\\s") // Noncompliant {{Replace this use of a reluctant quantifier with "[\\S\\s]*+".}}
//             ^^^^^

        // coverage
        Regex("(?:(?m))*?a")
        Regex("(?:(?m:.))*?(?:(?m))")

        // This replacement might not be equivalent in case of full match, but is equivalent in case of split
        Regex(".+?[^abc]") // Noncompliant {{Replace this use of a reluctant quantifier with "[abc]++".}}
//             ^^^
        Regex(".+?\\x{1F4A9}") // Noncompliant {{Replace this use of a reluctant quantifier with "[^\\x{1F4A9}]++".}}
//             ^^^
        Regex("<abc.*?>") // Noncompliant {{Replace this use of a reluctant quantifier with "[^>]*+".}}
//                 ^^^
        Regex("<.+?>|otherstuff") // Noncompliant {{Replace this use of a reluctant quantifier with "[^>]++".}}
//              ^^^
        Regex("(<.+?>)*") // Noncompliant {{Replace this use of a reluctant quantifier with "[^>]++".}}
//               ^^^
        Regex("\\S+?[abc]") // Noncompliant {{Replace this use of a reluctant quantifier with "[^abc\\s]++".}}
//             ^^^^^
        Regex("\\D+?[abc]") // Noncompliant {{Replace this use of a reluctant quantifier with "[^abc\\d]++".}}
//             ^^^^^
        Regex("\\w+?[abc]") // Noncompliant {{Replace this use of a reluctant quantifier with "[^abc\\W]++".}}
//             ^^^^^
        Regex("\\S*?[abc]") // Noncompliant {{Replace this use of a reluctant quantifier with "[^abc\\s]*+".}}
//             ^^^^^
        Regex("\\D*?[abc]") // Noncompliant {{Replace this use of a reluctant quantifier with "[^abc\\d]*+".}}
//             ^^^^^
        Regex("\\w*?[abc]") // Noncompliant {{Replace this use of a reluctant quantifier with "[^abc\\W]*+".}}
//             ^^^^^
        Regex("\\S+?[^abc]") // Noncompliant {{Replace this use of a reluctant quantifier with "[abc\\S]++".}}
//             ^^^^^
        Regex("\\s+?[^abc]") // Noncompliant {{Replace this use of a reluctant quantifier with "[abc\\s]++".}}
//             ^^^^^
    }

    fun compliant() {
        Regex("<[^>]++>")
        Regex("<[^>]+>")
        Regex("<[^>]+?>")
        Regex("<.{42}?>") // Adding a ? to a fixed quantifier is pointless, but also doesn't cause any backtracking issues
        Regex("<.+>")
        Regex("<.++>")
        Regex("<--.?-->")
        Regex("<--.+?-->")
        Regex("<--.*?-->")
        Regex("/\\*.?\\*/")
        Regex("<[^>]+>?")
        Regex("")
        Regex(".*?(?:a|b|c)") // Alternatives are currently not covered even if they contain only single characters
    }

    fun no_intersection() {
        Regex("<\\d+?>")
        Regex("<\\s+?>")
        Regex("<\\w+?>")
        Regex("<\\s{2,5}?>")
        Regex("<\\d{2,5}?>")
        Regex("<\\w{2,5}?>")
        Regex("\\d+?[abc]")
        Regex("\\s+?[abc]")
        Regex("\\W+?[abc]")
        Regex("\\W*?[abc]")
        Regex("\\s*?[abc]")
        Regex("\\d*?[abc]")
        Regex("\\d*?\\p{L}")
        Regex("\\d*?\\P{L}") // There is an intersection but we currently do not support p{.} and P{.}
        Regex("\\p{L}*?\\D") // There is an intersection but we currently do not support p{.} and P{.}
        Regex("\\P{L}*?\\d") // There is an intersection but we currently do not support p{.} and P{.}
    }
}
