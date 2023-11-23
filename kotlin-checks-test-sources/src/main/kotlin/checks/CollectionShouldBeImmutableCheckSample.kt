package checks

class CollectionShouldBeImmutableCheckSample {
    val protectedList  = mutableListOf(1,2,3) // compliant
    private val privateList  = mutableListOf(1,2,3) // compliant

    fun MutableCollection<Int>.doSomething(): Unit {} // Noncompliant

    //let also apply run with
    fun nonCompliant(): Unit {
        val x  = mutableListOf(1, 2, 3) // Noncompliant {{Make this collection immutable.}}
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
        id(x + listOf(1,2,3))
        val y = mutableSetOf("Iam", "Iam", "Iam") // Noncompliant
        y.contains("a")
        y.map { it + "a" }
        val z = mutableMapOf(1 to 2) // Noncompliant
        z[1]
        doNothing(x, y, z)
        val a = mutableListOf(1, 2, 3) // compliant, FN
        a.let { it.size }
        a.let { it.also { with(it, { this.size }) } }
        val b = mutableListOf(1, 2, 3) // Noncompliant
        Pair(1, b.reduce { acc, i -> i + acc  })
        val c = mutableListOf(1, 2, 3) // compliant, FN
        id(c)
    }

    fun List<Int>.toList(): List<Int> = this // compliant
    fun baz(list : MutableList<Int>): Unit {} // Noncompliant

    fun List<Int>.foo(): Unit {} // Compliant
    fun doNothing(a : List<Int>, b : Set<String>, c : Map<Int,Int>): Unit {}
    fun doNothing(a : List<Int>): List<Int> { return a } // Compliant

    fun <A>id(a : A): A = a // Compliant

    fun compliantScopedFunctions () {
        val a = mutableListOf(1,2,3) // Compliant
        a.let { it.add(1) }
        val b = mutableListOf(1,2,3) // Compliant
        b.also { it.add(1) }
        val c = mutableListOf(1,2,3) // Compliant
        c.apply { add(1) }
        val d = mutableListOf(1,2,3) // Compliant
        d.run { add(1) }
        val e = mutableListOf(1,2,3) // Compliant
        with(e) { add(1) }
        val f = mutableListOf(1,2,3) // Compliant
        f.let { it.let { it.let{ it.add(1) } } }
        val crazy = mutableListOf(1,2,3) // Compliant
        crazy.let { it.also { it.apply { it.run { with(this) { it.add(1) } } } } }
    }

    fun MutableList<Int>.doSomething2(): MutableList<Int> { return this } // Noncompliant

    fun compliantFunctionsCalledOn():Unit{
        val a = mutableListOf(1,2,3) // Compliant
        a.add(1)
        val b = mutableListOf(1,2,3) // Compliant
        b.iterator()
        val c : List<Int> = mutableListOf(1,2,3) // Compliant
        c.map { it + 1}
        val d = mutableSetOf(1,2,3) // Compliant
        d.add(1)
        val e =  mutableMapOf(1 to 2) // Compliant
        e.remove(1)
        val f = mutableListOf(1,2,3) // Compliant
        f.doSomething()
        val g = mutableListOf(1,2,3) // Compliant
        g.doSomething2()
        val h =  mutableSetOf(1,2,3) // Compliant
        h.doSomething()
        val i = mutableListOf(1) // Compliant
        ((i)).doSomething()
        val j = mutableListOf(1) // Compliant
        j.doSomething2().doSomething2().doSomething2()
        val k= mutableListOf(1) // Compliant
        when (k) {
            is MutableList<Int> -> k.doSomething()
        }
        val l = mutableListOf(1) // Compliant
        l!!.doSomething()
        val m = mutableListOf(1) // Compliant
        m?.doSomething()
        val n = mutableListOf(1) // Compliant
        if(true){n}else{n}.doSomething()
    }


    fun compliantBinaryExprCalledOn(): Unit {
        val c : List<Int> = mutableListOf(1,2,3) // Compliant
        c[0]
        val d = mutableListOf(1,2,3) // Compliant
        d += 1
        val e = mutableListOf(1,2,3) // Compliant
        e -= 1
        val f = mutableListOf(1,2,3) // Compliant
        f[0] = 1
        val g = mutableListOf(1,2,3) // Compliant
        (g)[0] = 1
    }



    fun compliantCalledInFun(): Unit {
        val a = mutableListOf(1,2,3) // Compliant
        foo_(a)
        val b = mutableSetOf(1,2,3) // Compliant
        foo2(b)
        val c = mutableListOf(1) // Compliant
        foo_(((c)))
        val d = mutableListOf(1) // Compliant
        foo_(d!!)
        val e = mutableListOf(1) // Compliant
        foo_(if(true){e}else{e})
        val f = mutableListOf(1) // Compliant
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


    fun foo(): Unit {
        val configure: (MutableMap<String, Any?>) -> Unit = {}
    }



}