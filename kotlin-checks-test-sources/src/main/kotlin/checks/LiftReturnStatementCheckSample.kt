package checks

class LiftReturnStatementCheckSample {

    fun returnIfElseNoncompliant(value: Int): String {
        if (value >= 0) { // Noncompliant {{Move "return" statements from all branches before "if" statement.}}
            return "positive"
        } else {
            return "negative"
        }
    }

    fun returnIfElseCompliant(value: Int): String {
        return if (value >= 0) { // Compliant
            "positive"
        } else {
            "negative"
        }
    }

    fun returnIfElseNoBlockNoncompliant(value: Int): String {
        if (value >= 0) return "positive" // Noncompliant {{Move "return" statements from all branches before "if" statement.}}
        else return "negative"
    }

    fun returnIfElseNoBlockCompliant(value: Int): String {
        return if (value >= 0) "positive" // Compliant
        else "negative"
    }

    fun returnIfWithoutElse(value: Int): String {
        if (value >= 0) return "positive" // Compliant
        return "negative"
    }

    fun returnIfNotFromElseBranch(value: Int): String {
        if (value >= 0) { // Compliant
            return "positive"
        } else {
            println("negative")
        }
        return "not from if"
    }

    fun returnIfNotFromThenBranch(value: Int): String {
        if (value >= 0) { // Compliant
            println("positive")
        } else {
            return "negative"
        }
        return "not from if"
    }

    fun noReturningIf(value: Int): String {
        if (value >= 0) { // Compliant
            println("positive")
        } else {
            println("negative")
        }
        return "not from if"
    }

    fun returnWhenElseNoncompliant(a: Float): Int {
        when { // Noncompliant {{Move "return" statements from all branches before "when" statement.}}
            a < 0 -> return -1
            a > 0 -> return 1
            else -> return 0
        }
    }

    fun returnWhenElseCompliant(a: Float): Int {
        return when { // Compliant
            a < 0 -> -1
            a > 0 -> 1
            else -> 0
        }
    }

    fun returnWhenNonExhaustive(a: Float): Int {
        when { // Compliant
            a < 0 -> return -1
            a > 0 -> return 1
        }
        return 0
    }

    fun returnIfInIfNoncompliant(value: Int): String {
        if (value >= -10) {
            if (value >= 0) { // Noncompliant {{Move "return" statements from all branches before "if" statement.}}
                return "positive"
            } else {
                return "negative"
            }
        }
        return "not from if"
    }

    fun returnIfInIfCompliant(value: Int): String {
        if (value >= -10) {
            return if (value >= 0) { // Compliant
                "positive"
            } else {
                "negative"
            }
        }
        return "not from if"
    }

    fun returnIfInWhenNoncompliant(value: Int): String {
        when (value) {
            10 -> if (value >= 0) { // Noncompliant {{Move "return" statements from all branches before "if" statement.}}
                return "positive"
            } else {
                return "negative"
            }

            20 -> println("20")
        }
        return "not from if"
    }

    fun returnIfInWhenComplaint(value: Int): String {
        when (value) {
            10 -> return if (value >= 0) { // Compliant
                "positive"
            } else {
                "negative"
            }

            20 -> println("20")
        }
        return "not from if"
    }

    fun returnWhenNotFromElseBranch(a: Float): Int {
        when { // Compliant
            a < 0 -> return -1
            a > 0 -> return 1
            else -> println(0)
        }
        return 0
    }

    fun returnWhenNotFromSecondBranch(a: Float): Int {
        when { // Compliant
            a < 0 -> return -1
            a > 0 -> println(1)
            else -> return 0
        }
        return 0
    }

    fun returnWhenOnlyFromSecondBranch(a: Float): Int {
        when { // Compliant
            a < 0 -> println(-1)
            a > 0 -> return 1
            else -> println(0)
        }
        return 0
    }

    enum class Color {
        RED,
        GREEN,
        BLUE
    }

    fun returnWhenExhaustiveEnumWithElseNoncomplaint(color: Color): Int {
        when (color) { // Noncompliant {{Move "return" statements from all branches before "when" statement.}}
            Color.RED, Color.GREEN -> return 3
            Color.BLUE -> return 4
            else -> return 0
        }
    }

    fun returnWhenNonExhaustiveEnumWithElseNoncomplaint(color: Color): Int {
        when (color) { // Noncompliant {{Move "return" statements from all branches before "when" statement.}}
            Color.RED, Color.GREEN -> return 3
            else -> return 0
        }
    }

    fun returnWhenExhaustiveEnumWithoutElseNoncomplaint(color: Color): Int {
        when (color) { // Noncompliant {{Move "return" statements from all branches before "when" statement.}}
            Color.RED, Color.GREEN -> return 3
            Color.BLUE -> return 4
        }
    }

    fun returnWhenExhaustiveEnumWithElseCompliant(color: Color): Int {
        return when (color) { // Compliant
            Color.RED, Color.GREEN -> 3
            Color.BLUE -> 4
            else -> 0
        }
    }

    fun returnWhenNonExhaustiveEnumWithElseCompliant(color: Color): Int {
        return when (color) { // Compliant
            Color.RED, Color.GREEN -> 3
            else -> 0
        }
    }

    fun returnWhenExhaustiveEnumWithoutElseCompliant(color: Color): Int {
        return when (color) { // Compliant
            Color.RED, Color.GREEN -> 3
            Color.BLUE -> 4
        }
    }

    fun returnWhenWithBlocks(color: Color): Int {
        when (color) { // Noncompliant {{Move "return" statements from all branches before "when" statement.}}
            Color.RED, Color.GREEN -> {
                return 3
            }

            Color.BLUE -> {
                return 4
            }
        }
    }

    fun ifWithElseAndNoIf() {
        if (true)
            else false
    }
}
