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

    fun allOperators(): Unit {
        val x = mutableListOf(1,2,3) // Noncompliant
        val y = mutableSetOf(1,2,3) // Noncompliant
        val z = mutableMapOf(1 to 2) // Noncompliant

        val a = mutableListOf(1,2,3) // Compliant
        a.addAll(listOf(1,2,3))
        val b = mutableListOf(1,2,3) // Compliant
        b.remove(1)
        val c = mutableListOf(1,2,3) // Compliant
        c.removeAll(listOf(1,2,3))
        val d = mutableListOf(1,2,3) // Compliant
        d.retainAll(listOf(1,2,3))
        val e = mutableListOf(1,2,3) // Compliant
        e.clear()
        val f = mutableListOf(1,2,3) // Compliant
        f.removeAt(0)
        val g = mutableMapOf(1 to 2) // Compliant
        g.put(1, 2)
        val h = mutableMapOf(1 to 2) // Compliant
        h.putAll(mapOf(1 to 2))
        val i = mutableSetOf(1,2,3) // Compliant
        i.remove(1)
        val j = mutableMapOf(1 to 2) // Compliant
        j[1] = 2
        val k = mutableListOf(1,2,3) // Compliant
        k += 1
        val l = mutableMapOf(1 to 2) // Compliant
        l.getOrPut(1) { 2 }
    }

    fun foo(): Unit {
        val configure: (MutableMap<String, Any?>) -> Unit = {}
    }

    fun returnCollection(l : MutableList<Int>): MutableList<Int> { // Noncompliant
        return l
    }


}