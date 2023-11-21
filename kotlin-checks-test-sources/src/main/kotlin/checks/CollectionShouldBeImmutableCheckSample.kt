package checks

class CollectionShouldBeImmutableCheckSample {


    fun append(): Unit {
        val list = mutableListOf(1,2,3) // compliant
        list.add(4)
        val list2 : MutableList<List<Int>?> = mutableListOf(listOf(1)) // compliant
        list2.add(null)
    }


    fun foo(x : MutableList<Int>): Unit {} // Noncompliant {{Make this collection immutable.}}
    fun callFun(): Unit {
        val x = mutableListOf(1,2,3) // compliant
        foo(x)
    }

    fun callFunParenthesis(): Unit {
        val x = mutableListOf(1,2,3) // compliant
        foo((x))
    }

    fun sum123Noncompliant(): Int {
        val list = mutableListOf(1,2,3) // Noncompliant
        return list.reduce { acc, it -> acc + it}
    }

    fun sum123Compliant(): Int {
        val list = listOf(1,2,3) // Compliant
        return list.reduce { acc, it -> acc + it}
    }

    fun sumListNoncompliant(list: MutableList<Int>): Int { // Noncompliant
        return list.reduce { acc, it -> acc + it}
    }

    fun sumListCompliant(list: List<Int>): Int { // Compliant
        return list.reduce { acc, it -> acc + it}
    }

    fun List<Int>.sumCompliant(): Int { // Compliant
        return reduce { acc, it -> acc + it }
    }

    fun MutableList<Int>.sumNoncompliant(): Int { // Noncompliant
//      ^^^^^^^^^^^^^^^^
        return reduce { acc, it -> acc + it}
    }

    fun f(x : List<Int>): Unit {}

    fun callConstructor(): Unit {
        val x = mutableListOf(1,2,3) // compliant
        val y = Pair(x, 1)
    }

    fun callConstructor2(): Unit {
        val x = mutableListOf(1,2,3) // Noncompliant
//      ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        val y = Pair(1, x.reduce { acc, i -> i + acc  })
    }


}