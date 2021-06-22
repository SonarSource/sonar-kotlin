package sample

@Suppress("Kotlin:Dummy")
class issueSuppressionSample2 { // Noncompliant
    fun someFun() {
        val unused = "foo" // Noncompliant
    }
}

class Foo2 {
    @SuppressWarnings("Kotlin:Dummy")
    fun _not_a_good_fun_name1_() { // Noncompliant

    }

    @SuppressWarnings(value = ["Kotlin:Dummy"])
    fun _not_a_good_fun_name2_() { // Noncompliant

    }

    fun someFun(@Suppress("Foo", "Kotlin:Dummy") ParameterWithBadName: String) { // Noncompliant
        val unusedAndReported = "bar" // Noncompliant

        @Suppress(names = ["Kotlin:Dummy"])
        val alsoUnused = "foo" // Noncompliant

        println("Hello Universe")
    }

    fun _bad_fun_name_2_() { // Noncompliant

    }
}
