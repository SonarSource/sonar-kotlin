package checks

import java.util.regex.Pattern

class EmptyLineRegexCheck {
    
    fun kotlin_Regex(str: String) {
        val regex1 = Regex("^$", RegexOption.MULTILINE) // Noncompliant {{Remove MULTILINE mode or change the regex.}}
//                         ^^^^

        regex1.find(str)
//                  ^^^<

        val regex2 = "^$".toRegex(RegexOption.MULTILINE) // Noncompliant {{Remove MULTILINE mode or change the regex.}}
//                   ^^^^

        regex2.find(str)
//                  ^^^<


        Regex("(?mx)^$").find(str) // Noncompliant
        Regex("(?mi)^$").find(str) // Noncompliant
        Regex("(?m:^$)").find(str) // Noncompliant

        Regex("(?m:^$)").matchEntire(str) // Compliant

    }
    
    fun tested_for_emptiness(str: String, str2: String) {
        val p4 = Pattern.compile("(?m)^$") // Compliant, tested for emptiness
        val b4 = p4.matcher(str).find() || str.isEmpty()

        val p5 = Regex("(?m)^$") // Compliant, tested for emptiness
        val b5 = p5.find(str2) ?: (str2 != "")

    }
    
    fun non_compliant_pattern_assigned(str: String) {
        val p1 = Pattern.compile("^$", Pattern.MULTILINE) // Noncompliant {{Remove MULTILINE mode or change the regex.}}
//                               ^^^^
        p1.matcher(str).find() 
//                 ^^^<
        p1.matcher(str).find()
//                 ^^^<
        val p2 = Pattern.compile("(?m)^$") // Noncompliant
//                               ^^^^^^^^
        p2.matcher(str).find()
        val p3 = Pattern.compile("(?m)^$", Pattern.MULTILINE) // Noncompliant
        p3.matcher(str).find()
    }

    fun non_compliant_pattern_directly_used(str: String?) {
        Pattern.compile("^$", Pattern.MULTILINE).matcher(str).find() // Noncompliant
//                      ^^^^
        Pattern.compile("(^$)", Pattern.MULTILINE).matcher(str).find() // Noncompliant
        Pattern.compile("(?:^$)", Pattern.MULTILINE).matcher(str).find() // Noncompliant
        Pattern.compile("(?m)^$").matcher(str).find() // Noncompliant
        Pattern.compile("(?m)(?x)^$").matcher(str).find() // Noncompliant
        Pattern.compile("(?m)(^$)").matcher(str).find() // Noncompliant
        Pattern.compile("(?m)^$", Pattern.MULTILINE).matcher(str).find() // Noncompliant
        Pattern.compile("(?mx)^$").matcher(str).find() // Noncompliant
        Pattern.compile("(?mi)^$").matcher(str).find() // Noncompliant
        Pattern.compile("(?m:^$)").matcher(str).find() // Noncompliant
        Pattern.compile("^$", Pattern.MULTILINE or Pattern.COMMENTS).matcher(str).find() // Noncompliant
        Pattern.compile("^ $", Pattern.MULTILINE or Pattern.COMMENTS).matcher(str).find() // Noncompliant
        Pattern.compile("^$|empty", Pattern.MULTILINE).matcher(str).find() // Noncompliant
        Pattern.compile("(e)(^$)|(?m)^$").matcher(str).find() // Noncompliant
    }

    fun nonCompliantOnString(str: String?) {
        Pattern.compile("^$", Pattern.MULTILINE).matcher("").find() // Noncompliant
        val p1 = Pattern.compile("^$", Pattern.MULTILINE) // Noncompliant
//                               ^^^^        
        val b1 = p1.matcher("notEmpty").find()
        val b2 = p1.matcher("").find()
//                          ^^<        
    }

    fun not_used_in_problematic_situations(str: String?) {
        val p1 = Pattern.compile("^$") // Compliant, not a multiline
        val b1 = p1.matcher(str).find()
        val p2 = Pattern.compile("^$", Pattern.LITERAL) // Compliant, not a multiline
        val b2 = p2.matcher(str).matches()
        val p2_2 = Pattern.compile("^$", 0x10) // Compliant, not a multiline
        val b2_2 = p2_2.matcher(str).matches()
        val p2_3 = Pattern.compile("^$", MY_FLAG) // Compliant, not a multiline
        val b2_3 = p2_3.matcher(str).matches()
        Pattern.compile("^$").matcher(str).find() // Compliant, no multiline flag
        val p3 = Pattern.compile("^$", Pattern.MULTILINE) // Compliant, not used with find
        val b3 = p3.matcher(str).matches()
        val p4 = Pattern.compile("(?m)^$") // Compliant, not used with find
        val b4 = p4.matcher(str).matches()
        val p5 = Pattern.compile("regex", Pattern.MULTILINE) // Compliant, not empty line regex
        val b5 = p5.matcher(str).find()
        Pattern.compile("^$", Pattern.MULTILINE).matcher(str).matches() // Compliant, not used with find
        Pattern.compile("^|$", Pattern.MULTILINE).matcher(str).find() // Compliant
        Pattern.compile("e^|\$e", Pattern.MULTILINE).matcher(str).find() // Compliant
        Pattern.compile("^\$e", Pattern.MULTILINE).matcher(str).find() // Compliant
        Pattern.compile("^e$", Pattern.MULTILINE).matcher(str).find() // Compliant
        Pattern.compile("e^$", Pattern.MULTILINE).matcher(str).find() // Compliant
        Pattern.compile("e(^$)", Pattern.MULTILINE).matcher(str).find() // Compliant
        Pattern.compile("ee(^$)", Pattern.MULTILINE).matcher(str).find() // Compliant
        Pattern.compile("(^$)e", Pattern.MULTILINE).matcher(str).find() // Compliant
        Pattern.compile("(^$)ee", Pattern.MULTILINE).matcher(str).find() // Compliant
        Pattern.compile("(e)(^$)", Pattern.MULTILINE).matcher(str).find() // Compliant
        Pattern.compile("(e)(^)($)", Pattern.MULTILINE).matcher(str).find() // Compliant
        Pattern.compile("[a-c]^$", Pattern.MULTILINE).matcher(str).find() // Compliant
        Pattern.compile("[a-c](^$)", Pattern.MULTILINE).matcher(str).find() // Compliant
        Pattern.compile("e(?m:^$)", Pattern.MULTILINE).matcher(str).find() // Compliant
        Pattern.compile("ee(?m:^$)", Pattern.MULTILINE).matcher(str).find() // Compliant
        Pattern.compile("(?m:e^)(?m:\$e)", Pattern.MULTILINE).matcher(str).find() // Compliant
        Pattern.compile("(?m)(e^$)").matcher(str).find() // Compliant
        Pattern.compile("^^", Pattern.MULTILINE).matcher(str).find() // Compliant
        Pattern.compile("$$", Pattern.MULTILINE).matcher(str).find() // Compliant
        Pattern.compile("$\\B", Pattern.MULTILINE).matcher(str).find() // Compliant
        Pattern.compile("(?m:(?-m:^$))").matcher(str).find() // Compliant

        // 3 FN, we don't expect anyone writing this.
        Pattern.compile("(?:^)(?:$)", Pattern.MULTILINE).matcher(str).find() // FN
        Pattern.compile("(?m:^)(?m:$)").matcher(str).find() // FN
        Pattern.compile("(^)($)", Pattern.MULTILINE).matcher(str).find() // FN
    }

    fun tested_for_emptiness_2(str: String): Boolean {
        val p4 = Pattern.compile("(?m)^$") // Compliant, tested for emptiness
        return if (str.isEmpty()) {
            true
        } else p4.matcher(str).find()
    }

    fun tested_for_emptiness_3(str: String): Boolean {
        return if (str.isEmpty()) {
            true
        } else Pattern.compile("(?m)^$").matcher(str).find()
    }

    fun tested_for_emptiness_4(str: String): Boolean {
        val p4 = Pattern.compile("(?m)^$") // FN, we consider any test for emptiness to be compliant
        if (str.isEmpty()) {
            println("str is empty!")
        }
        return p4.matcher(str).find()
    }

    fun not_tested_for_emptiness(str1: String, str2: String?): Boolean {
        val p4 = Pattern.compile("(?m)^$") // Noncompliant [[secondary=136]]
        return if (str1.isEmpty()) {
            false
        } else p4.matcher(str1).find()
            && p4.matcher(str2).find()
    }

    fun not_identifier(str1: String?) {

        Pattern.compile("^$", Pattern.MULTILINE).matcher(EMPTY_CONSTANT).find() // Noncompliant
        Regex("^$", RegexOption.MULTILINE).find(EMPTY_CONSTANT)  // Noncompliant

        // Compliant, don't report on fields to avoid FP.
        Pattern.compile("^$", Pattern.MULTILINE).matcher(field).find()
        Regex("^$", RegexOption.MULTILINE).find(field)
    }

    fun from_variable() {
        val str = string
        Pattern.compile("^$", Pattern.MULTILINE).matcher(str).find() // Noncompliant
    }

    fun from_variable_compliant() {
        val str = string
        if (str.isEmpty()) {
            return
        }
        Pattern.compile("^$", Pattern.MULTILINE).matcher(str).find() // Compliant
    }

    fun in_replace(str: String) {
        val s1 = str.replace("(?m)^$".toRegex(), "Empty") // Noncompliant
//                           ^^^^^^^^^^^^^^^^^^        
        val s2 = str.replace("^$".toRegex(), "Empty") // Compliant
        val s3 = "".replace("(?m)^$".toRegex(), "Empty") // Noncompliant
        val s4 = str.replace("(?m)^$".toRegex(), "Empty") // Noncompliant
        val s5 = str.replaceFirst("(?m)^$".toRegex(), "Empty") // Noncompliant
        val s6 = str.replaceFirst("^$".toRegex(), "Empty") // Compliant
        val s7 = "".replaceFirst("(?m)^$".toRegex(), "Empty") // Noncompliant
        val s8 = str.replaceFirst("(?m)^$".toRegex(), "Empty") // Noncompliant

        val regex = "(?m)^$".toRegex()
//                           ^^^^^^^^^>        
        val s9 = str.replaceFirst(regex, "Empty") // Noncompliant
//                                ^^^^^   


        val regex10 = Regex("(?m)^$")
//                    ^^^^^^^^^^^^^^^>        
        val s10 = str.replaceFirst(regex10, "Empty") // Noncompliant
//                                 ^^^^^^^   


        val regex11 = Regex("(?m)^$", RegexOption.MULTILINE)
//                    ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^>        
        val s11 = str.replaceFirst(regex11, "Empty") // Noncompliant
//                                 ^^^^^^^   


    }

    fun in_replace_compliant(str: String) {
        if (str.isEmpty()) {
            return
        }
        val s1 = str.replace("(?m)^$".toRegex(), "Empty") // Compliant
        val s2 = str.replaceFirst("(?m)^$".toRegex(), "Empty") // Compliant
        val s3 = str.replace("(?m)^$".toRegex(), "Empty") // Compliant
    }

    fun in_replace_all_compliant_2(str: String) {
        val s1 = if (str.isEmpty()) "Empty" else str.replace("(?m)^$".toRegex(), "Empty") // Compliant
        val s2 = if (str.isEmpty() || str.substring(1) == "") "Empty" else str.replace("(?m)^$".toRegex(), "Empty") // Compliant
    }

    fun in_matches(str: String) {
        // When used in other context (with matches), mistakes are still possible, but we are not supporting it as it is really unlikely to happen.
        val b = str.matches("(?m).*^$.*".toRegex()) // Compliant, FN
        Pattern.matches("(?m).*^$.*", str)
    }

    val string: String
        get() = ""

    companion object {
        private const val MY_FLAG = 0x10
        private const val EMPTY_CONSTANT = ""
        private const val NON_EMPTY_CONSTANT = ""
    }

    val field = "Empty"
}
