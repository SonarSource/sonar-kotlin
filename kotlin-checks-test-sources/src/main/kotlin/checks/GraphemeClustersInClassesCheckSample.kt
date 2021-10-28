package checks

class GraphemeClustersInClassesCheckSample {
    fun noncompliant(str: String?) {

        // Note that the location/highlighting comments in this file look confusing, as the highlighting may appear to be longer than the
        // code that is supposed to be highlighted. This is not actually the case, though, as the unicode characters triggering this rule
        // are actually multiple characters that occupy multiple columns. Your IDE/editor is simply smart enough to display the characters
        // as a single one in a single column, even though the chars really occupy multiple columns.

        Regex("[aaaèaaa]") // Noncompliant {{Extract 1 Grapheme Cluster(s) from this character class.}}
//             ^^^^^^^^^^
        Regex("[0Ṩ0]") // Noncompliant {{Extract 1 Grapheme Cluster(s) from this character class.}}
//             ^^^^^^^
        Regex("aaa[è]aaa") // Noncompliant
//                ^^^^
        // two secondary per line: one for the regex location, and one for the cluster location
        Regex("[èaèaè]") // Noncompliant {{Extract 3 Grapheme Cluster(s) from this character class.}}
//             ^^^^^^^^^^
        Regex("[èa-dä]") // Noncompliant
        Regex(
            "[èa]" +  // Noncompliant
                "aaa" +
                "[dè]" // Noncompliant
        )
        "abc".replaceFirst("[ä]".toRegex(), "A") // Noncompliant
        Regex("[c̈]") // Noncompliant
        Regex("[e⃝]") // Noncompliant

        Regex("""[èaèaè]""") // Noncompliant {{Extract 3 Grapheme Cluster(s) from this character class.}}
//               ^^^^^^^^^^
        Regex("""[èa-dä]""") // Noncompliant

    }

    fun compliant(str: String?) {
        Regex("[é]") // Compliant, a single char
        Regex("[e\u0300]") // Compliant, escaped unicode
        Regex("[e\\u0300]") // Compliant, escaped unicode
        Regex("[e\\x{0300}]") // Compliant, escaped unicode
        Regex("[e\u20DD̀]") // Compliant, (letter, escaped unicode, mark) can not be combined
        Regex("[\u0300e]") // Compliant, escaped unicode, letter
        Regex("[̀̀]") // Compliant, two marks
        Regex("[̀̀]") // Compliant, one mark
        Regex("ä") // Compliant, not in a class
    }
}
