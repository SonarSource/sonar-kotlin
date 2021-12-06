package checks

class DuplicatesInCharacterClassCheckSample {
    fun nonCompliant() {

        Regex("""[\"12\".]""") // Noncompliant {{Remove duplicates in this character class.}}
//      ^^^^^> {{Function call of which the argument is interpreted as regular expression.}}
//                ^^@-1
//                    ^^@-2< {{Additional duplicate}}


        Regex("[\\\"12\\\".]") // Noncompliant
//      ^^^^^>  ^^^^  ^^^^<

        Regex("""[0-9x9]""") // Noncompliant {{Remove duplicates in this character class.}}
//      ^^^^^>    ^^^ ^<


        Regex("[9x0-9]") // Noncompliant
//      ^^^^^>  ^ ^^^<


        Regex("[0-7x3-9]") // Noncompliant
//      ^^^^^>  ^^^ ^^^<


        Regex("[0-93-57]") // Noncompliant
        Regex("[4-92-68]") // Noncompliant
        Regex("[0-33-9]") // Noncompliant
        Regex("[0-70-9]") // Noncompliant
        Regex("[3-90-7]") // Noncompliant
        Regex("[3-50-9]") // Noncompliant
        Regex("[xxx]") // Noncompliant
        Regex("[A-z_]") // Noncompliant
        Regex("(?i)[A-Za-z]") // Noncompliant
        Regex("(?i)[A-_d]") // Noncompliant
        Regex("(?iu)[Ä-Üä]") // Noncompliant
        Regex("(?iu)[a-Öö]") // Noncompliant
        Regex("[  ]") // Noncompliant
        Regex("(?i)[  ]") // Noncompliant
        Regex("(?iu)[  ]") // Noncompliant
        Regex("(?i)[A-_D]") // Noncompliant
        Regex("(?iu)[A-_D]") // Noncompliant
        Regex("(?i)[xX]") // Noncompliant
        Regex("(?iu)[äÄ]") // Noncompliant
        Regex("(?iU)[äÄ]") // Noncompliant
        Regex("(?iu)[xX]") // Noncompliant

        Regex("[\\x{1F600}-\\x{1F637}x\\x{1F608}]") // Noncompliant
//      ^^^^^>  ^^^^^^^^^^^^^^^^^^^^^ ^^^^^^^^^^<

        Regex("[\\Qxx\\E]") // Noncompliant
        Regex("[[a][a]]") // Noncompliant
        Regex("[[abc][b]]") // Noncompliant
        Regex("[[^a]b]") // Noncompliant
        Regex("[[^a]z]") // Noncompliant
        Regex("[a[^z]]") // Noncompliant
        Regex("[z[^a]]") // Noncompliant

        Regex("[\\s\\Sx]") // Noncompliant
//                 ^^^

        Regex("(?U)[\\s\\Sx]") // Noncompliant
//                     ^^^

        Regex("[\\w\\d]") // Noncompliant
        Regex("[\\wa]") // Noncompliant
        Regex("[\\d1]") // Noncompliant
        Regex("[\\d1-3]") // Noncompliant
        Regex("(?U)[\\wa]") // Noncompliant
        Regex("(?U)[\\s\\u0085" +  // Noncompliant
            "\\u2028" +
            "\\u2029]")
        Regex("[0-9" +  // Noncompliant
            "9]")
        Regex("[a-b" +
            "0-9" +  // Noncompliant
//           ^^^
            "d-e" +
            "9]")
        Regex("[a-z" +  // Noncompliant
            "0-9" +
            "b" +
            "9]")
        Regex("[a-z" +  // Noncompliant
            "0-9" +
            "b" +
            "c" +
            "9]")
        Regex("[ba-zc]") // Noncompliant
        // Miss "b" in secondary locations
        Regex("[aba-z]") // Noncompliant
        Regex("[aba-zc]") // Noncompliant
        Regex("[a-c" +  // Noncompliant
            "b" +
            "a-z" +
            "d]")
        Regex("[0-54-6]") // Noncompliant
        Regex("[0-352-6]") // Noncompliant

        Regex("[0-392-43-54-65-76-87-9]") // Noncompliant
//              ^^^

        Regex("[0-397-96-85-72-44-63-5]") // Noncompliant
        Regex("[0-397-96-8" +  // Noncompliant
            "a" +  // not included
            "5-72-44-63-5]")


        Regex("(?i)[A-_d-{]") // Noncompliant
        Regex("(?i)[A-z_]") // Noncompliant
    }

    fun compliant() {
        Regex("a-z\\d")
        Regex("[0-9][0-9]?")
        Regex("[xX]")
        Regex("[\\s\\S]")
        Regex("[[^\\s\\S]x]")
        Regex("(?U)[\\s\\S]")
        Regex("(?U)[\\S\\u0085\\u2028\\u2029]")
        Regex("[\\d\\D]")
        Regex("(?U)[\\d\\D]")
        Regex("[\\w\\W]")
        Regex("(?U)[\\w\\W]")
        Regex("[\\wä]")
        Regex("(?i)[äÄ]")
        Regex("(?i)[Ä-Üä]")
        Regex("(?u)[äÄ]")
        Regex("(?u)[xX]")
        Regex("[ab-z]")
        Regex("[[a][b]]")
        Regex("[[^a]a]")
        Regex("(?i)[a-Öö]")
        Regex("[0-9\\Q.-_\\E]") // This used to falsely interpret .-_ as a range and complain that it overlaps with 0-9
        Regex("[A-Z\\Q-_.\\E]")
        Regex("[\\x00\\x01]]") // This used to falsely complain about x and 0 being duplicates
        Regex("[\\x00-\\x01\\x02-\\x03]]")
        Regex("[z-a9-0]") // Illegal character class should not make the check explode
        Regex("[aa") // Check should not run on syntactically invalid regexen
        Regex("(?U)[\\wä]") // False negative because we don't support Unicode characters in \\w and \\W
        Regex("(?U)[[^\\W]a]") // False negative because once we negate a character class whose contents we don't
        // fully understand, we ignore it to avoid false positives
        Regex("[\\N{slightly smiling face}\\N{slightly smiling face}]") // FN because we don't support \\N
        Regex("[[a-z&&b-e]c]") // FN because we don't support intersections
        Regex("[\\p{IsLatin}x]") // FN because we don't support \p at the moment
    }


    fun emoji(str: java.lang.String) {
        Regex("[😂😊]") // Compliant
        Regex("[^\ud800\udc00-\udbff\udfff]") // Compliant
    }
}
