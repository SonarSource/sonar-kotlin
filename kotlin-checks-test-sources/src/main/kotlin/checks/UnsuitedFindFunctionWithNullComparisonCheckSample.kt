package checks

class UnsuitedFindFunctionWithNullComparisonCheckSample {

    fun onCollections(list: List<Int>, set: Set<Int>, array: Array<Int>, map: Map<Int, Int>) {

        list.find { it > 5 } != null // Noncompliant {{Replace with `list.any { it > 5 }`.}}
    //  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^

        list.findLast { it > 5 } == null // Noncompliant  {{Replace with `list.none { it > 5 }`.}}
        list.firstOrNull { it > 5 } == null // Noncompliant  {{Replace with `list.none { it > 5 }`.}}
        list.lastOrNull { it > 5 } == null // Noncompliant  {{Replace with `list.none { it > 5 }`.}}

        list.find { it == 5 } != null // Noncompliant  {{Replace with `list.contains(5)`.}}
        list.find { 5 == it } != null // Noncompliant  {{Replace with `list.contains(5)`.}}
        list.find { it != 5 } == null // Noncompliant  {{Replace with `list.none { it != 5 }`.}}
        list.find { 5 != it } == null // Noncompliant  {{Replace with `list.none { 5 != it }`.}}

        list.find { it == 5 } == null // Noncompliant  {{Replace with `!list.contains(5)`.}}
        list.find { 5 == it } == null // Noncompliant  {{Replace with `!list.contains(5)`.}}
        list.find { it != 5 } != null // Noncompliant  {{Replace with `list.any { it != 5 }`.}}
        list.find { 5 != it } != null // Noncompliant  {{Replace with `list.any { 5 != it }`.}}

        fun five() = 5
        val five = 5
        list.find { it > five } == null // Noncompliant  {{Replace with `list.none { it > five }`.}}
        list.find { it > five() } == null // Noncompliant  {{Replace with `list.none { it > five() }`.}}
        list.find { it == five } != null // Noncompliant  {{Replace with `list.contains(five)`.}}
        list.find { it == five() } == null // Noncompliant  {{Replace with `!list.contains(five())`.}}

        list.find { it > 5 }
        list.findLast { it > 5 }
        list.find { it == five }
        list.find { it > five() }

        list.find { it > 5 } == 5
        list.findLast { it > 5 } == 5
        list.find { it == 5 } == five
        list.find { it > five } == five()

        set.find { it > 5 } != null // Noncompliant
        set.findLast { it > 5 } == null  // Noncompliant
        set.find { it == 5 } != null // Noncompliant
        set.find { it > five } == null // Noncompliant

        array.find { it > 5 } != null // Noncompliant
        array.findLast { it > 5 } == null  // Noncompliant
        array.find { it == 5 } != null // Noncompliant
        array.find { it > five } == null // Noncompliant

        map.entries.find { it.key > 5 } != null // Noncompliant
        map.entries.findLast { it.key > 5 } != null // Noncompliant
        map.entries.find { it.key == five } != null // Noncompliant
        map.entries.find { it.key > five() } == null // Noncompliant

        list.map { it + 1 }.filter { it > 1 }.find { it > 5 } != null // Noncompliant {{Replace with `list.map { it + 1 }.filter { it > 1 }.any { it > 5 }`.}}
    //  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

        list.findLast { `my var` -> `my var` == 5 } != null // Noncompliant {{Replace with `list.contains(5)`.}}


        null != list.find { it > 5 } // Noncompliant {{Replace with `list.any { it > 5 }`.}}

        ((((list.find { it > 5 })))) != null // Noncompliant {{Replace with `list.any { it > 5 }`.}}

        with(list) {
            if (find { it > 5 } != null) { // Noncompliant {{Replace with `any { it > 5 }`.}}

            }

            if (find { it == 5 } != null) { // Noncompliant {{Replace with `contains(5)`.}}

            }

            if (any { it > 5 }) {

            }
        }

        5.let {
            with(list) {
                findLast { it > 5 } != null // Noncompliant {{Replace with `any { it > 5 }`.}}
            }
        }

        list.filter { x -> x == 5 && (list.firstOrNull { it is Int } as Int?) == null } // Noncompliant {{Replace with `list.none { it is Int }`.}}

        list.filter { x -> // Noncompliant {{Replace with `any`.}}
            if(x == 5) false else true // filter
        }.map {
            it.inc() // mapping
        }.find {
            it > 5
        } != null

        list.filter { x -> // Noncompliant {{Replace with `contains`.}}
            if(x == 5) false else true // filter
        }.map {
            it.inc() // mapping
        }.find {
            it == 5
        } != null

        val nullableList :List<Int>? = null

        nullableList?.findLast { it == 5 } != null // Noncompliant {{Replace with `nullableList?.contains(5)`.}}
        nullableList?.firstOrNull { it >= 5 } != null // Noncompliant {{Replace with `nullableList?.any { it >= 5 }`.}}
        nullableList?.find { it >= 5 } == null // Noncompliant {{Replace with `nullableList?.none { it >= 5 }`.}}

        nullableList!!.findLast { it == 5 } != null // Noncompliant {{Replace with `nullableList!!.contains(5)`.}}

        list.find { x -> x == 5 } != null // Noncompliant {{Replace with `list.contains(5)`.}}
        list.find { x -> x == five } != null // Noncompliant {{Replace with `list.contains(five)`.}}
        list.find { x -> five() == 5 } != null // Noncompliant {{Replace with `list.any { x -> five() == 5 }`.}}
        list.find { x -> x == five && five == 5 } != null // Noncompliant
        list.find { x -> x == five && x == 5 } != null // Noncompliant
        list.find { x -> 1 == 5 } != null // Noncompliant
        list.find { x -> { x > 5 }() } != null // Noncompliant {{Replace with `list.any { x -> { x > 5 }() }`.}}
        list.findLast { x -> { x == 5 }() } != null // Noncompliant {{Replace with `list.any { x -> { x == 5 }() }`.}}
    }

    class NonKotlinCollections {
        private fun <T> find(list: List<T>, filter: (T) -> Boolean): T? {
            return list.find(filter)
        }

        private fun List<Int>.find(x: Int): Int {
            return this.indexOf(x)
        }

        private fun <T> List<T>.find(biFilter: (T, T) -> Boolean): T? {
            return this.get(0)
        }
        private fun <T> List<T>.findLast(mapper: (T) -> T): T? {
            return this.get(0)
        }

        // All Compliant because these are not defined in `kotlin.collections` package
        fun onFunction(list: List<Int>) {
            find(list) { it > 5 } != null
            find(list) { it < 5 } == null
            find(list) { it == 5 } != null
            list.find(5) != null
            list.find({ x, y -> x == y }) != null
            list.find({ x, y -> x > y }) != null
            list.findLast{ x -> x + 1 } != null
        }
    }

}
