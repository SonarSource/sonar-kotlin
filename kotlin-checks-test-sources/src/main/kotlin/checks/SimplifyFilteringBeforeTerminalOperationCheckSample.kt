package checks

class SimplifyFilteringBeforeTerminalOperationCheckSample {
    fun test(list: List<Int>, set: Set<Int>, array: Array<Int>) {
        list.filter { it > 5 }.any() // Noncompliant {{Remove "filter { it > 5 }" and replace "any()" with "any { it > 5 }".}}
        //   ^^^^^^^^^^^^^^^^^
        list.any { it > 5 }

        set.filter { it > 5 }.any() // Noncompliant
        set.any { it > 5 }

        array.filter { it > 5 }.any() // Noncompliant
        array.any { it > 5 }

        list.filter { it > 5 }.none() // Noncompliant
        list.none { it > 5 }

        list.filter { it > 5 }.count() // Noncompliant
        list.count { it > 5 }

        list.filter { it > 5 }.last() // Noncompliant
        list.last { it > 5 }

        list.filter { it > 5 }.lastOrNull() // Noncompliant
        list.lastOrNull { it > 5 }

        list.filter { it > 5 }.first() // Noncompliant
        list.first { it > 5 }

        list.filter { it > 5 }.firstOrNull() // Noncompliant
        list.firstOrNull { it > 5 }

        list.filter { it > 5 }.single() // Noncompliant
        list.single { it > 5 }

        list.filter { it > 5 }.singleOrNull() // Noncompliant
        list.singleOrNull { it > 5 }

        list.map { it + 1 }.filter { it < 10 }.filter { it > 5 }.any() // Noncompliant
        list.map { it + 1 }.filter { it < 10 }.filter { it > 5 }.map { it }.any()

        with(list) {
            filter { it > 5 }.any() // Noncompliant {{Remove "filter { it > 5 }" and replace "any()" with "any { it > 5 }".}}
        //  ^^^^^^^^^^^^^^^^^
        }

        (list.filter { it > 5 }).any() // Noncompliant {{Remove "filter { it > 5 }" and replace "any()" with "any { it > 5 }".}}
        //    ^^^^^^^^^^^^^^^^^

        list.any()
        list.none()
        list.count()
        list.last()
        list.lastOrNull()
        list.first()
        list.firstOrNull()
        list.single()
        list.singleOrNull()

        list.filter { it < 10 }.any { it > 5 }
        list.filter { it > 5 }.apply { any() }
        list.filter { it > 5 }.let { it.any() }
    }

    fun onMap(map: Map<Int, Int>) {
        map.filter { it.value > 5 }.any() // Noncompliant
        map.any { it.value > 5 }

        map.filter { it.value > 5 }.none() // Noncompliant
        map.none { it.value > 5 }

        map.filter { it.value > 5 }.count() // Noncompliant
        map.count { it.value > 5 }

        map.filter { (key, value) -> key > 5 && value > 5 }.any() // Noncompliant
        map.any { (key, value) -> key > 5 && value > 5 }

        map.filter { (key, value) -> key > 5 && value > 5 }.none()  // Noncompliant
        map.none { (key, value) -> key > 5 && value > 5 }

        map.filter { (key, value) -> key > 5 && value > 5 }.count() // Noncompliant
        map.count { (key, value) -> key > 5 && value > 5 }
    }
}
