package checks

@Deprecated("Some text")
enum class MyEnumClass(val value: String) {
    ENTRY1(""), // Compliant (FN?) since Kotlin 1.7, where the compiler doesn't seem to find this anymore.
}

class Example : DeprecatedCode() // Noncompliant {{Deprecated code should not be used.}}
//              ^^^^^^^^^^^^^^

    @DeprecatedAnnotation() // Noncompliant
//   ^^^^^^^^^^^^^^^^^^^^
class DeprecatedCodeUsedCheckSample {

    fun usesDeprecated(kStr: DeprecatedString): String { // Noncompliant
//                           ^^^^^^^^^^^^^^^^

        DeprecatedConstructor("") // Noncompliant

        val d = DeprecatedCode() // Noncompliant
//              ^^^^^^^^^^^^^^

        d.prop // Noncompliant
//        ^^^^

        d.prop2 = "" // Noncompliant
//        ^^^^^
        println(d.prop2) // Compliant, only setter is deprecated

        println(d.prop3) // Noncompliant
//                ^^^^^


        deprecatedFunction() // Noncompliant
//      ^^^^^^^^^^^^^^^^^^

        return kStr - "" // Noncompliant
    }

}

class DeprecatedConstructor {
    @Deprecated("")
    constructor(s: String) {
    }
}

@Deprecated("")
open class DeprecatedCode {

    @Deprecated("")
    val prop: String = ""

    var prop2: String = ""
        @Deprecated("") set
    var prop3: String = ""
        @Deprecated("") get

}

@Deprecated("")
fun deprecatedFunction() {}

@Deprecated("")
annotation class DeprecatedAnnotation

@Deprecated("")
typealias DeprecatedString = String

@Deprecated("")
private operator fun DeprecatedString.minus(s: String) = this + s // Noncompliant

class DeprecatedParameterUsedInFollowingParameter(
    @Deprecated("This is deprecated") val deprecatedParameter: String, // Compliant: not used, but declared
    val anotherParameterUsingDeprecatedOne: Int = deprecatedParameter.length, // Compliant: what is deprecated is the generated property, not the parameter itself
) {
    val x = deprecatedParameter.length // Noncompliant

    init {
        println(deprecatedParameter.length) // Noncompliant
    }
}

// region top-level non compliant scenario

@Deprecated("This function is deprecated, use newFunction instead", ReplaceWith("deprecatedCodeUsed_newFunction()"))
fun deprecatedCodeUsed_topLevel() {
    println("This is the old function.")
}

fun deprecatedCodeUsed_newFunction() {
    println("This is the new function.")
}

var deprecatedCodeUsed_var = deprecatedCodeUsed_topLevel() // Noncompliant

// endregion

fun nestedFunctions() {
    @Deprecated("This function is deprecated, use newFunction instead", ReplaceWith("newFunction()"))
    fun oldFunction() {
        println("This is the old function.")
    }

    fun newFunction() {
        println("This is the new function.")
    }

    oldFunction() // FN
}
