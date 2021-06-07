package checks

open class TooManyParametersCheckSample(p1: Int, p2: Int) {

    constructor(p1: Int, p2: Int, p3: Int) : this(p1, p2)

    constructor() : this(0, 0, 0)

    // Noncompliant@+1 {{This function has 3 parameters, which is greater than the 1 authorized.}}
    open fun sample(p1: Int, p2: Int, p3: Int) {
//           ^^^^^^          ^^^^^^^< ^^^^^^^<

        // TODO false-positive
        // Noncompliant@+1 {{This function has 2 parameters, which is greater than the 1 authorized.}}
        { p1: Int, p2: Int -> }
//      ^^^^^^^^^^^^^^^^^^^^^^^
    }

    fun compliant(p1: Int) = Unit

    // TODO false-negative
    fun String.extension(p1: Int, p2: Int) = Unit

    annotation class GetMapping

    @GetMapping
    fun annotated(p1: Int, p2: Int) = Unit

    @checks.TooManyParametersCheckSample.GetMapping
    fun annotated2(p1: Int, p2: Int) = Unit

    abstract class OverrideIsCompliant : TooManyParametersCheckSample() {
        override fun sample(p1: Int, p2: Int, p3: Int) = Unit
    }

}
