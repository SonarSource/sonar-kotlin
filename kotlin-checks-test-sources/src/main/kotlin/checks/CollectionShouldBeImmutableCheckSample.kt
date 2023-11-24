package checks

class CollectionShouldBeImmutableCheckSample {
    fun MutableCollection<Int>.doSomething(): Unit {} // Noncompliant

    //let also apply run with
    fun nonCompliant(
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

    fun List<Int>.foo(): Unit {} // Compliant
    fun doNothing(a : List<Int>, b : Set<String>, c : Map<Int,Int>): Unit {}
    fun doNothing(a : List<Int>): List<Int> { return a } // Compliant

    fun <A>id(a : A): A = a // Compliant

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

    fun MutableList<Int>.doSomething2(): MutableList<Int> { return this } // Noncompliant

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

    fun MutableList<Int>.noncompliantDelegate(): Int { // Noncompliant
//      ^^^^^^^^^^^^^^^^
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

    fun id(x : AMutableCollections): AMutableCollections = x // compliant, for now don't know how to check that is subtype of MutableCollection


    fun foo(configure: (MutableMap<String, Any?>) -> Unit): Unit { // compliant
    }

}