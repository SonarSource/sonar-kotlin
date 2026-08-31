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

    fun usesDeprecated(kStr: DeprecatedString): String {

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
interface DeprecatedInterface

@Deprecated("")
fun deprecatedFunction() {}

@Deprecated("")
annotation class DeprecatedAnnotation

@Deprecated("")
typealias DeprecatedString = String

@Deprecated("")
private operator fun DeprecatedString.minus(s: String) = this + s // Compliant - enclosing function is deprecated (cluster 6)

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

// deprecated type in structural (non-executable) positions should not be flagged

fun returnsDeprecated(): DeprecatedCode? = null // Compliant - return type

class UsesDeprecatedInSignatures {
    val field: DeprecatedCode? = null // Compliant - field type
    fun withTypeArg(list: List<DeprecatedString>) {} // Compliant - type argument
}

// region actual usages of deprecated types must be reported

// constructor call
class ExtendsDeprecatedClass : DeprecatedCode() // Noncompliant

// interface supertype
class ImplementsDeprecatedInterface : DeprecatedInterface // Noncompliant

// by-delegation
class DelegatesViaDeprecatedInterface(d: DeprecatedInterface) : DeprecatedInterface by d // Noncompliant
//                                                              ^^^^^^^^^^^^^^^^^^^

fun typeChecks(x: Any) {
    // is-check
    if (x is DeprecatedCode) {} // Noncompliant
    // cast
    val y = x as DeprecatedCode // Noncompliant
    // when is-pattern
    when (x) {
        is DeprecatedCode -> {} // Noncompliant
        else -> {}
    }
}

//  annotation without parentheses must not be suppressed as type reference
@DeprecatedAnnotation // Noncompliant
class AnnotatedWithDeprecatedAnnotation

// endregion

// region nested usage of deprecated code within a deprecated scope

@Deprecated("This whole class is deprecated")
class DeprecatedClassUsingOtherDeprecated {
    // field declaration in deprecated class references another deprecated type
    val member: DeprecatedCode = DeprecatedCode() // Compliant - enclosing class is deprecated

    // calling other deprecated APIs from within the deprecated class body
    fun usesOtherDeprecatedApis() {
        deprecatedFunction() // Compliant - enclosing class is deprecated
        val x = DeprecatedCode() // Compliant - enclosing class is deprecated
    }

    companion object {
        // factory method in companion object calling its own deprecated class constructor
        fun create(): DeprecatedClassUsingOtherDeprecated = DeprecatedClassUsingOtherDeprecated() // Compliant - enclosing class is deprecated
    }
}

@Deprecated("Use newWay instead")
fun deprecatedFunctionCallingOtherDeprecated() {
    deprecatedFunction() // Compliant - enclosing function is deprecated
    DeprecatedCode() // Compliant - enclosing function is deprecated
}

// endregion
