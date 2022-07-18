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

        //Noncompliant@+1
        val d = DeprecatedCode("") // Noncompliant
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
//             ^^^^^^^^^
    }

}

@Deprecated("")
open class DeprecatedCode {

    @Deprecated("")
    constructor(s: String) {
    }

    constructor() {}

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
