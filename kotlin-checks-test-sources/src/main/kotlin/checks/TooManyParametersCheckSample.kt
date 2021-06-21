package checks

open class TooManyParametersCheckSample(p1: Int, p2: Int, p3: Int) {

    constructor(p1: Int, p2: Int, p3: Int, p4: Int) : this(p1, p2, p3)

    constructor() : this(0, 0, 0, 0)

    // Noncompliant@+1 {{This function has 4 parameters, which is greater than the 2 authorized.}}
    open fun sample(p1: Int, p2: Int, p3: Int, p4: Int) {
//           ^^^^^^                   ^^^^^^^< ^^^^^^^<
    }

    fun compliant(p1: Int) = Unit

    // Noncompliant@+1 {{This function has 3 parameters, which is greater than the 2 authorized.}}
    fun String.extension(p1: Int, p2: Int, p3: Int) = Unit

    annotation class GetMapping

    @GetMapping
    fun annotated(p1: Int, p2: Int, p3: Int) = Unit

    @checks.TooManyParametersCheckSample.GetMapping
    fun annotated2(p1: Int, p2: Int, p3: Int) = Unit

    abstract class OverrideIsCompliant : TooManyParametersCheckSample() {
        override fun sample(p1: Int, p2: Int, p3: Int, p4: Int) = Unit
    }

    fun lambda(
        n: (Int, Int, Int) -> Int, // Noncompliant {{This function has 3 parameters, which is greater than the 2 authorized.}}
//         ^^^^^^^^^^^^^^^^^^^^^^
        c: (Int, Int) -> Int,
    ) {
        lambda({ p1, p2, p3 -> p1 + p2 + p3 }, c)
    }

    // Noncompliant@+1 {{This function has 3 parameters, which is greater than the 2 authorized.}}
    val anonymousFunction = fun(p1: Int, p2: Int, p3: Int) = p1 + p2 + p3
//                          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

}
