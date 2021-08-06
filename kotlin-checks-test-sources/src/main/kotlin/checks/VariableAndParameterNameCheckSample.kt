package checks

class VariableAndParameterNameCheckSample(
  var PARAM:Int, // Noncompliant {{Rename this parameter to match the regular expression ^[_a-z][a-zA-Z0-9]*$}}
//    ^^^^^
  var param: Int,
  var `backticks`: Int, // Noncompliant
//    ^^^^^^^^^^^
) {

    constructor(
        PARAM: Int, // Noncompliant
    ) : this(PARAM, PARAM, PARAM)

    val NON_LOCAL = ""

    fun example(
        PARAM: String, // Noncompliant
        c: Collection<Pair<Any, Any>>,
    ) {
        val LOCAL = "" // Noncompliant
        val local = ""

        { PARAM: Int, param: Int, _: Int -> // Noncompliant 
//        ^^^^^
        }

        c.joinToString { (A, B) -> "" }

        val LOCAL_DELEGATE // Noncompliant 
            by lazy { "" }
    }

    fun String.extension(
        PARAM: String, // Noncompliant
    ) {
        val LOCAL = ""// Noncompliant 

        { PARAM: String -> // Noncompliant
            var LOCAL = "" // Noncompliant
        }
    }

}
