package sample


class issueSuppressionSample3 { // Noncompliant
    fun someFun() {
        val unused = "foo" // Noncompliant
    }
}

class Foo3 {
    @SuppressWarnings("Different:Rule")
    fun _not_a_good_fun_name1_() { // Noncompliant

    }

    @SuppressWarnings(value = ["Different:Rule"])
    fun _not_a_good_fun_name2_() { // Noncompliant

    }

    fun someFun(@Suppress("Foo", "Different:Rule") ParameterWithBadName: String) { // Noncompliant
        val unusedAndReported = "bar" // Noncompliant


        val alsoUnused = "foo" // Noncompliant

        println("Hello Universe")
    }

    fun _bad_fun_name_2_() { // Noncompliant

    }
}

private fun S1479(values: List<Int>): List<String> {
    return values.map { value ->
        when (value) { // Noncompliant
            0 -> "0"
            1 -> "0"
            2 -> "0"
            3 -> "0"
            4 -> "0"
            5 -> "0"
            6 -> "0"
            7 -> "0"
            8 -> "0"
            9 -> "0"
            10 -> "0"
            11 -> "0"
            12 -> "0"
            13 -> "0"
            14 -> "0"
            15 -> "0"
            16 -> "0"
            17 -> "0"
            18 -> "0"
            19 -> "0"
            20 -> "0"
            21 -> "0"
            22 -> "0"
            23 -> "0"
            24 -> "0"
            25 -> "0"
            26 -> "0"
            27 -> "0"
            28 -> "0"
            29 -> "0"
            30 -> "0"
            else -> "Unknown"
        }
    }
}
