package checks

import kotlin.math.max
import kotlin.properties.Delegates

class VarShouldBeValCheckSample {
    private var foo = 0 // Noncompliant {{Replace the keyword `var` with `val`. This property is never modified.}}
    private val bar = 0 // compliant
    private lateinit var late : Address // compliant
    var notUsed = "not used" // compliant, not a local variable
    private var y = "y" // Noncompliant
        set(value){
            field = value
        }
    private var abc = 0 // compliant

    fun classProperties(): Unit {
        this::abc.set(1)
    }


    class Address {
        var street: String = "baker" // compliant
        var number: Int = 221 // compliant
    }

    annotation class Fancy

    fun assignmentOperators(): Unit {
        @Fancy
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
        var (a, b) = Pair(0, 1) // Noncompliant
//      ^^^
        var (e, f) = Pair(0, 1) // Compliant
        e = 0
        var c = 0 // compliant
        c = 1 as Int
    }

    private var oneSelect = "used" // compliant
    private var twoSelect = "used" // compliant

    fun mySelf() = this

    fun assignmentExpressions(): Unit {
        this.oneSelect = "used"
        this.mySelf().twoSelect = "used"

        var delegate1 by Delegates.notNull<String>() // Noncompliant
        var delegate2 by Delegates.notNull<String>() // compliant
        delegate2 = "used"


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
        var newLength = max(16, 2) // Noncompliant {{Replace the keyword `var` with `val`. This property is never modified.}}
        return newLength
    }

    fun nested(): Int {
        var shadowed = 0 // Noncompliant
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
