package checks

import kotlin.math.max

class VarShouldBeValCheckSampleNoSemantics {
    var notUsed = "not used" // compliant, not a local variable
    var x = "x" // compliant
        set(value){
            x = value
        }

    class Address {
        var street: String = "baker" // compliant
        var number: Int = 221 // compliant
    }

    fun assignmentOperators(): Unit {
        var j = 0 // Noncompliant
        var x = 0 // compliant
        x = 1
        var y = 0 // compliant
        y += 1
        var p = 0 // compliant
        (p) += 1
        var i = 0 // compliant
        i -= 1
        var z = 0 // compliant
        z++
        var w = 0 // compliant
        w--
        var k = 0 // compliant
        ++k
        var l = 0 // compliant
        --l
        var v = 0 // compliant
        v *= 2
        var u = 0 // compliant
        u/= 2
        var t = 0 // compliant
        t %= 2
        var (a, b) = Pair(0, 1) // not supported for now, should we ?
        a = 1
        var c = 0 // compliant
        c = 1 as Int
    }

    fun assignmentExpressions(): Unit {
        val numbers = mutableListOf(1, 2, 3, 4)
        numbers[0] = 2
        var x = 0 // compliant
        (x) = 1
        var y = 0 // compliant
        ((y)) += 1
        val z = object {
            var z = 0
        }
        z.z = 10

        val address = Address()
        address.number ++


    }

    fun resizeNonCompliant(): Int {
        var newLength = max(16, 2) // Noncompliant {{Replace the keyword `var` with `val`.}}
        return newLength
    }

    fun nested(): Int {
        var shadowed = 0 // compliant, cannot be found with no semantics
        var z = 0 // compliant
        fun nestedFun(x : Int): Unit {
           var shadowed = 1 // compliant
           shadowed = 2
           z = 2
        }

        var y = 0 // compliant
        if(true){
            y = 1
        }

        return shadowed
    }


    fun resizeCompliant(): Int {
        val newLength = max(16, 2) // Compliant
        return newLength
    }
}
