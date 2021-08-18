package checks

// No issues without semantics
class UnusedLocalVariableCheckSampleNoSemantics {

    val f =  { i: Int ->
        if (i == 0) {
            val toExpr = " "
        }
    }
    
    val hcek = 0

    init {
        var i: Int
    }

    constructor() {
        val i = 90
    }
    var global = 0

    fun fooBar() {
        val a = 0

        val b: String

        var c: String

        var d: Int
        d = 0

        var e: Int
        e = d + a


        val f =  { i: Int ->
            if (i == 0) {
                val toExpr = " "
            }
        }
        
        f(0)
        
        fun fff() {
            var b = 0
        }
    }

    fun String.extension() {
        val unused = 0
    }
}

