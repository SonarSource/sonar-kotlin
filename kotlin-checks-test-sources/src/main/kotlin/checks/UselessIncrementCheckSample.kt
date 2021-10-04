package checks

private var globalVariable = 0

class UselessIncrementCheckSample {
    fun foo(choice: Boolean): Int {
        var i = 0
        var j = 0
        if (choice) {
            i = j++ // Compliant, not the same variable
            i = i++ // Noncompliant {{Remove this increment or correct the code not to waste it.}}
//          ^^^^^^^
            return j++ // Noncompliant
//                 ^^^
        } else {
            i++
            return ++j // Compliant, prefix expression
        }

    }

    class A {
        var i = 0
        fun foo(): Int {
            return i++ // Compliant, it's not a variable but a field
        }
        fun bar(): Int {
            return globalVariable++ // Compliant, it's not a local variable
        }
    }

    fun bar(arg: Any?): Any {
        return arg!! // Compliant, postfix expression but not ++ or --
    }

    fun coverage(): Boolean {
        var i = 0
        i = 1

        class A(var i: Int = 0)

        val a = A()
        i = a.i++
        a.i = i++
        return i == 2
    }
}
