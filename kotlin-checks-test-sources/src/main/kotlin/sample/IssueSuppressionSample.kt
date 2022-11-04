package sample

@Suppress("Kotlin:Dummy")
class issueSuppressionSample1 { // Compliant
    fun someFun() {
        val unused = "foo" // Compliant (suppressed @ class level)
    }
}

class Foo1 {
    @SuppressWarnings("Kotlin:Dummy")
    fun _not_a_good_fun_name1_() { // Compliant

    }

    @SuppressWarnings(value = ["Kotlin:Dummy"])
    fun _not_a_good_fun_name2_() { // Compliant

    }

    fun someFun(@Suppress("Foo", "Kotlin:Dummy") ParameterWithBadName: String) { // Compliant
        val unusedAndReported = "bar" // Noncompliant

        @Suppress(names = ["Kotlin:Dummy"])
        val alsoUnused = "foo" // Compliant

        println("Hello Universe")
    }

    fun _bad_fun_name_2_() { // Noncompliant

    }
}

private fun S1479(values: List<Int>): List<String> {
    return values.map { value ->
        @Suppress("Kotlin:Dummy")
        when (value) { // suppressed therefore compliant
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
