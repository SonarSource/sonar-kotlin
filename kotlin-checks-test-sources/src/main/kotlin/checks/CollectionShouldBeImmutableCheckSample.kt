package checks

fun compliantScopedFunctions(
    a: MutableList<Int>, // Compliant
    b: MutableList<Int>, // Compliant
    c: MutableList<Int>, // Compliant
    d: MutableList<Int>, // Compliant
    e: MutableList<Int>, // Compliant
    f: MutableList<Int>, // Compliant
    crazy: MutableList<Int>,
): Unit { // Compliant
    a.let { it.add(1) }
    b.also { it.add(1) }
    c.apply { add(1) }
    d.run { add(1) }
    with(e) { add(1) }
    f.let { it.let { it.let { it.add(1) } } }
    crazy.let { it.also { it.apply { it.run { with(this) { it.add(1) } } } } }
}

class CollectionShouldBeImmutableCheckSample {
    fun MutableCollection<Int>.doSomething(): Unit {} // Don't report extension functions

    //let also apply run with
    fun nonCompliantXYZ(
        x: MutableList<Int>, // Noncompliant {{Make this collection immutable.}}
        y: MutableSet<String>,  // Noncompliant
//      ^^^^^^^^^^^^^^^^^^^^^
        z: MutableMap<Int, Int>, // Noncompliant
        a: MutableList<Int>,  // compliant, FN
        b: MutableList<Int>, // Noncompliant
        c: MutableList<Int>, // compliant, FN
    ): Unit {
        baz(x.toList().toMutableList())
        baz(doNothing(x).toMutableList())
        doNothing(x)
        x.reduce { acc, it -> acc + it }
        x + 1
        x.size
        for (i in x) {
            println(i)
        }
        x.foo()
        id(x + listOf(1, 2, 3))
        y.contains("a")
        y.map { it + "a" }
        z[1]
        doNothing(x, y, z)
        a.let { it.size }
        a.let { it.also { with(it, { this.size }) } }
        Pair(1, b.reduce { acc, i -> i + acc })
        id(c)
    }

    fun List<Int>.toList(): List<Int> = this // compliant
    fun baz(list : MutableList<Int>): Unit {} // Noncompliant


    //Declared on immutable
    fun List<Int>.foo(): Unit {} // Compliant
    fun doNothing(a : List<Int>, b : Set<String>, c : Map<Int,Int>): Unit {}
    fun doNothing(a : List<Int>): List<Int> { return a } // Compliant

    fun <A>id(a : A): A = a // Compliant

    fun MutableList<Int>.doSomething2(): MutableList<Int> { return this } // Don't report extension functions

    fun compliantFunctionsCalledOn(
        a: MutableList<Int>, // Compliant
        b: MutableList<Int>, // Compliant
        c: List<Int>, // Compliant
        d: MutableList<Int>, // Compliant
        e: MutableMap<Int, Int>, // Compliant
        f: MutableList<Int>, // Compliant
        g: MutableList<Int>, // Compliant
        h: MutableSet<Int>, // Compliant
        i: MutableList<Int>, // Compliant
        j: MutableList<Int>, // Compliant
        k: MutableList<Int>, // Compliant
        l: MutableList<Int>, // Compliant
        m: MutableList<Int>, // Compliant
        n: MutableList<Int>, // Compliant
        o: MutableMap<Int,Int>, // Compliant
    ): Unit {
        a.add(1)
        b.iterator()
        c.map { it + 1}
        d.add(1)
        e.remove(1)
        f.doSomething()
        g.doSomething2()
        h.doSomething()
        ((i)).doSomething()
        j.doSomething2().doSomething2().doSomething2()
        when (k) {
            is MutableList<Int> -> k.doSomething()
        }
        l!!.doSomething()
        m?.doSomething()
        if(true){n}else{n}.doSomething()
        o.entries
    }


    fun compliantBinaryExprCalledOn(
        c: List<Int>, // Compliant
        d: MutableList<Int>, // Compliant
        e: MutableList<Int>, // Compliant
        f: MutableList<Int>, // Compliant
        g: MutableList<Int>, // Compliant
    ): Unit {
        c[0]
        d += 1
        e -= 1
        f[0] = 1
        (g)[0] = 1
    }



    fun compliantCalledInFun(
        a: MutableList<Int>, // Compliant
        b: MutableList<Int>, // Compliant
        c: MutableList<Int>, // Compliant
        d: MutableList<Int>, // Compliant
        e: MutableList<Int>, // Compliant
        f: MutableList<Int> // Compliant
    ): Unit {
        foo_(a)
        foo2(b)
        foo_(((c)))
        foo_(d!!)
        foo_(if(true){e}else{e})
        Pair(f, 1)
    }

    fun foo_(x : MutableList<Int>): Unit {} // Noncompliant {{Make this collection immutable.}}
    fun foo2(x : MutableCollection<Int>): Unit {} // Noncompliant


    fun nonCompliantParameter(list: MutableList<Int>): Int { // Noncompliant
        return list.reduce { acc, it -> acc + it}
    }

    fun compliantParameter(list: List<Int>): Int { // Compliant
        return list.reduce { acc, it -> acc + it}
    }

    fun MutableList<Int>.compliantDelegate(): Int { // Compliant
        this.add(1)
        return reduce { acc, it -> acc + it }
    }


    fun MutableList<Int>.compliantDelegate2(): Unit { // Compliant
        foo_(this)
    }

    fun MutableList<Int>.compliantDelegate3(): Unit { // Compliant
        foo_(this)
        add(1)
    }

    fun MutableList<Int>.compliantDelegate4(): Unit { // Compliant
        if(add(1)) {}
    }

    fun MutableList<Int>.noncompliantDelegate(): Int { // Don't report extension functions
        return reduce { acc, it -> acc + it}
    }

    fun MutableList<Int>.noncompliantDelegate2(): Int { // compliant, FN
        var list = mutableListOf(1, 2, 3) // we should change noReciever to check that it is called on this
        list.add(1)
        return reduce { acc, it -> acc + it}
    }



    class AMutableCollections : MutableCollection<Int> {
        override val size: Int
            get() = TODO()

        override fun contains(element: Int): Boolean {
            TODO()
        }

        override fun containsAll(elements: Collection<Int>): Boolean {
            TODO()
        }

        override fun isEmpty(): Boolean {
            TODO()
        }

        override fun add(element: Int): Boolean {
            TODO()
        }

        override fun addAll(elements: Collection<Int>): Boolean {
            TODO()
        }

        override fun clear() {
            TODO()
        }

        override fun iterator(): MutableIterator<Int> {
            TODO()
        }

        override fun remove(element: Int): Boolean {
            TODO()
        }

        override fun removeAll(elements: Collection<Int>): Boolean {
            TODO()
        }

        override fun retainAll(elements: Collection<Int>): Boolean {
            TODO()
        }
    }

    fun id(x : AMutableCollections): AMutableCollections = x // FN,  for now don't know how to check that is subtype of MutableCollection


    fun foo(configure: (MutableMap<String, Any?>) -> Unit): Unit { // compliant
    }

    interface A {
        fun foo(list : MutableList<Int>): Unit // compliant
        fun bar(list : MutableList<Int>): Int { // Noncompliant
            return list.reduce { acc, it -> acc + it}
        }
    }

    abstract class B : A{
        override fun foo(list: MutableList<Int>) { // compliant
        }

        abstract fun baz(list: MutableList<Int>): Unit // compliant

        open fun qux(list: MutableList<Int>): Unit { // compliant
        }

    }

}

private fun nonCompliantParameterOnFileLevel(list: MutableList<Int>): Int { // Noncompliant
    return list.reduce { acc, it -> acc + it}
}

// https://sonarsource.atlassian.net/browse/SONARKT-388
private fun <T> intersectionType(t: T) = if (t is String) listOf(t) else emptyList()

fun sum123(acc: List<Int>): Int {
    val list = mutableListOf(1,2,3) // Noncompliant
    val list2: List<Int> = mutableListOf(1,2,3) // Compliant, immutable type specified

    list2.size
    return list.reduce { acc, item -> acc + item}
}
