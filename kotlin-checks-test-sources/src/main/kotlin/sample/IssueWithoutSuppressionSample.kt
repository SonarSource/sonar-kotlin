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
