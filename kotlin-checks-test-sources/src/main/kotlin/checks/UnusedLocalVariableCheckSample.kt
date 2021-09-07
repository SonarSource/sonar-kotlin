package checks

class UnusedLocalVariableCheckSample {

    val f =  { i: Int ->
        if (i == 0) {
            val toExpr = " " // Noncompliant
        }
    }
    
    val hcek = 0 // Compliant, is not local

    init {
        var i: Int // Noncompliant
    }

    constructor() {
        val i = 90 // Noncompliant
    }
    var global = 0       // Compliant

    fun fooBar() {
        val a = 0    // Compliant

        val b: String        // Noncompliant {{Remove this unused "b" local variable.}}
//          ^

        var c: String        // Noncompliant {{Remove this unused "c" local variable.}}
//          ^

        var d: Int       // Compliant
        d = 0

        var e: Int        // Compliant
        e = d + a


        val f =  { i: Int ->
            if (i == 0) {
                val toExpr = " " // Noncompliant
            }
        }
        
        f(0)
        
        fun fff() {
            var b = 0 // Noncompliant
        }
    }

    fun String.extension() {
        val unused = 0 // Noncompliant
    }
}

class HappyPath {
    init {
        var i: Int = 101
        print(i)
    }

    constructor() {
        val i = 90
        print(i)
    }
}

data class D(val p: Int)

fun duplicatedNameInInitializer(d: D) {
    val p = d.p // Noncompliant {{Remove this unused "p" local variable.}}
//      ^
}

fun destructuringDeclaration(d: D) {
    val (p) = d // Noncompliant {{Remove this unused "p" local variable.}}
//       ^
}
