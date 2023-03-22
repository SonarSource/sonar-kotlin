package checks

class MergeIfElseIntoWhenCheckSample {

    fun returnOneIfCompliant(value: Int): String {
        return if (value > 0) { // Complaint
            "positive"
        } else {
            "negative"
        }
    }

    fun returnOneIfNoElseCompliant(value: Int): String {
        if (value > 0) { // Complaint
            return "positive"
        }
        return "negative"
    }

    fun returnTwoIfNoncompliant(value: Int): String {
        return if (value > 0) { // Noncompliant {{Merge chained "if" statements into a single "when" statement.}}
            "positive"
        } else if (value < 0) {
            "negative"
        } else {
            "zero"
        }
    }

    fun returnTwoIfComplaint(value: Int): String {
        return when { // Compliant
            value > 0 -> "positive"
            value < 0 -> "negative"
            else -> "zero"
        }
    }

    fun returnTwoIfNoElseNoncompliant(value: Int): String {
        if (value > 0) { // Noncompliant {{Merge chained "if" statements into a single "when" statement.}}
            return "positive"
        } else if (value < 0) {
            return "negative"
        }
        return "zero"
    }

    fun returnTwoIfNoElseComplaint(value: Int): String {
        when { // Compliant
            value > 0 -> return "positive"
            value < 0 -> return "negative"

        }
        return "zero"
    }

    fun returnThreeIfNoncompliant(value: Int): String {
        return if (value == 10) { // Noncompliant {{Merge chained "if" statements into a single "when" statement.}}
            "ten"
        } else if (value > 0) {
            "positive"
        } else if (value < 0) {
            "negative"
        } else {
            "zero"
        }
    }

    fun returnThreeIfComplaint(value: Int): String {
        return when { // Compliant
            value == 10 -> "ten"
            value > 0 -> "positive"
            value < 0 -> "negative"
            else -> "zero"
        }
    }

    fun returnThreeIfNoElseNoncompliant(value: Int): String {
        if (value == 10) { // Noncompliant {{Merge chained "if" statements into a single "when" statement.}}
            return "ten"
        } else if (value > 0) {
            return "positive"
        } else if (value < 0) {
            return "negative"
        }
        return "zero"
    }

    fun returnThreeIfNoElseComplaint(value: Int): String {
        when { // Compliant
            value == 10 -> return "ten"
            value > 0 -> return "positive"
            value < 0 -> return "negative"
        }
        return "zero"
    }

    fun returnIfInIfWithoutElseNonCompliant(value: Int): String {
        if (value >= 0) { // Compliant
            if (value == 10) { // Noncompliant {{Merge chained "if" statements into a single "when" statement.}}
                return "ten"
            } else if (value == 0) {
                return "zero"
            }
            return "positive"
        }
        return "negative"
    }

    fun returnIfInIfWithoutElseCompliant(value: Int): String {
        if (value >= 0) { // Compliant
            when { // Compliant
                value == 10 -> return "ten"
                value == 0 -> return "zero"
            }
            return "positive"
        }
        return "negative"
    }

    fun returnIfInIfWithElseNonCompliant(value: Int): String {
        if (value >= 0) { // Noncompliant {{Merge chained "if" statements into a single "when" statement.}}
            if (value == 10) { // Noncompliant {{Merge chained "if" statements into a single "when" statement.}}
                return "ten"
            } else if (value == 0) {
                return "zero"
            }
            return "positive"
        } else if (value < -10) {
            return "below -10"
        }
        return "negative"
    }

    fun returnIfInIfWithElseCompliant(value: Int): String {
        when {
            value >= 0 -> { // Comliant
                when { // Compliant
                    value == 10 -> return "ten"
                    value == 0 -> return "zero"
                }
                return "positive"
            }
            value < -10 -> {
                return "below -10"
            }
        }
        return "negative"
    }

    fun returnIfInIfWithElseNonCompliantX(value: Int): String {
        if (value >= 0) { // Compliant
            if (value == 10) { // Compliant
                return "ten"
            } else {
                if (value == 0) {
                    return "zero"
                }
            }
            return "positive"
        } else {
            if (value < -10) {
                return "below -10"
            }
        }
        return "negative"
    }
}
