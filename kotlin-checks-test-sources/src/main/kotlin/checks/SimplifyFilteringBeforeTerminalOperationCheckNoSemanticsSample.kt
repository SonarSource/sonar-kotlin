package checks

class SimplifyFilteringBeforeTerminalOperationCheckNoSemanticsSample {
    fun test(list: List<Int>, set: Set<Int>, array: Array<Int>) {
        list.filter { it > 5 }.any()
        list.any { it > 5 }

        set.filter { it > 5 }.any()
        set.any { it > 5 }

        array.filter { it > 5 }.any()
        array.any { it > 5 }

        list.filter { it > 5 }.none()
        list.none { it > 5 }

        list.filter { it > 5 }.count()
        list.count { it > 5 }

        list.filter { it > 5 }.last()
        list.last { it > 5 }

        list.filter { it > 5 }.lastOrNull()
        list.lastOrNull { it > 5 }

        list.filter { it > 5 }.first()
        list.first { it > 5 }

        list.filter { it > 5 }.firstOrNull()
        list.firstOrNull { it > 5 }

        list.filter { it > 5 }.single()
        list.single { it > 5 }

        list.filter { it > 5 }.singleOrNull()
        list.singleOrNull { it > 5 }

        list.map { it + 1 }.filter { it < 10 }.filter { it > 5 }.any()
        list.map { it + 1 }.filter { it < 10 }.filter { it > 5 }.map { it }.any()

        with(list) {
            filter { it > 5 }.any()
        }

        (list.filter { it > 5 }).any()

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
        map.filter { it.value > 5 }.any()
        map.any { it.value > 5 }

        map.filter { it.value > 5 }.none()
        map.none { it.value > 5 }

        map.filter { it.value > 5 }.count()
        map.count { it.value > 5 }

        map.filter { (key, value) -> key > 5 && value > 5 }.any()
        map.any { (key, value) -> key > 5 && value > 5 }

        map.filter { (key, value) -> key > 5 && value > 5 }.none()
        map.none { (key, value) -> key > 5 && value > 5 }

        map.filter { (key, value) -> key > 5 && value > 5 }.count()
        map.count { (key, value) -> key > 5 && value > 5 }
    }
}
