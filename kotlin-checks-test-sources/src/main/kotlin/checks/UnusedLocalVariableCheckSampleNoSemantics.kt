package checks

// No issues without semantics
class UnusedLocalVariableCheckSampleNoSemantics {

    val f =  { i: Int ->
        if (i == 0) {
            val toExpr = " " // Noncompliant
//              ^^^^^^
        }
    }
    
    val hcek = 0

    init {
        var i: Int // Noncompliant
//          ^
    }

    constructor() {
        val i = 90 // Noncompliant
///         ^
    }
    var global = 0

    fun fooBar() {
        val a = 0

        val b: String // Noncompliant
//          ^

        var c: String // Noncompliant
//          ^

        var d: Int
        d = 0

        var e: Int
        e = d + a


        val f =  { i: Int ->
            if (i == 0) {
                val toExpr = " " // Noncompliant
//                  ^^^^^^
            }
        }
        
        f(0)
        
        fun fff() {
            var b = 0 // Noncompliant
//              ^
        }
    }

    fun String.extension() {
        val unused = 0 // Noncompliant
//          ^^^^^^
    }
}

