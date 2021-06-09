package checks

class VariableAndParameterNameCheckSample(
  var PARAM:Int, // Noncompliant {{Rename this parameter to match the regular expression "^[_a-z][a-zA-Z0-9]*$".}}
//    ^^^^^
  var param: Int,
  var `backticks`: Int, // Noncompliant {{Rename this parameter to match the regular expression "^[_a-z][a-zA-Z0-9]*$".}}
//    ^^^^^^^^^^^
) {

    constructor(
        PARAM: Int, // Noncompliant {{Rename this parameter to match the regular expression "^[_a-z][a-zA-Z0-9]*$".}}
    ) : this(PARAM, PARAM, PARAM)

    val NON_LOCAL = ""

    fun example(
        PARAM: String, // Noncompliant {{Rename this parameter to match the regular expression "^[_a-z][a-zA-Z0-9]*$".}}
        c: Collection<Pair<Any, Any>>,
    ) {
        val LOCAL = "" // Noncompliant {{Rename this local variable to match the regular expression "^[_a-z][a-zA-Z0-9]*$".}}
        val local = ""

        { PARAM: Int, param: Int, _: Int -> // Noncompliant {{Rename this parameter to match the regular expression "^[_a-z][a-zA-Z0-9]*$".}}
//        ^^^^^
        }

        c.joinToString { (A, B) -> "" }

        // TODO false-negative
        val LOCAL_DELEGATE by lazy { "" }
    }

    // TODO false-negatives
    fun String.extension(
        PARAM: String,
    ) {
        val LOCAL = ""

        { PARAM: String -> // Noncompliant
            var LOCAL = "" // Noncompliant
        }
    }

}
