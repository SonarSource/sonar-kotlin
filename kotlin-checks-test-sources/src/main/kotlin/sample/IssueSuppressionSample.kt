package sample


@Deprecated("")
class D

@Suppress("DEPRECATION")
fun f() {
    D() // Compliant
}

@Suppress("UNUSED", "kotlin:S101")
class issueSuppressionSample1 { // Compliant
    fun someFun() {
        val unused = "foo" // Compliant (suppressed @ class level)
    }
}

class Foo1 {
    @SuppressWarnings("kotlin:S100")
    fun _not_a_good_fun_name1_() { // Compliant

    }

    @SuppressWarnings(value = ["kotlin:S100"])
    fun _not_a_good_fun_name2_() { // Compliant

    }

    fun someFun(@Suppress("Foo", "kotlin:S117") ParameterWithBadName: String) { // Compliant
        val unusedAndReported = "bar" // Noncompliant

        @Suppress(names = ["kotlin:S1481"])
        val alsoUnused = "foo" // Compliant

        println("Hello Universe")
    }

    fun _bad_fun_name_2_() { // Noncompliant

    }
}

private fun S1479(values: List<Int>): List<String> {
    return values.map { value ->
        @Suppress("kotlin:S1479")
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

private fun UselessNullChecks(foo: String) {
    @Suppress("kotlin:S6619")
    foo ?: "bar"

    @Suppress("UNNECESSARY_SAFE_CALL")
    foo?.plus("bar")

    @Suppress("UNNECESSARY_NOT_NULL_ASSERTION")
    foo != null

    @Suppress("USELESS_ELVIS")
    foo ?: "bar"

    foo ?: "bar" // Noncompliant
}
