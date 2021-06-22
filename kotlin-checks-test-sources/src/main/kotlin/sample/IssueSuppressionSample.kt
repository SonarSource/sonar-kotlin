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
