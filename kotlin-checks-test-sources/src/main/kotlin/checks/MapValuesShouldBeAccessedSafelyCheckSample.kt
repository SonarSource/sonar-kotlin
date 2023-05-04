package checks

class MapValuesShouldBeAccessedSafelyCheckSample {
    val l1 = mapOf(1 to "one", 2 to "two", 3 to "five").get(1) // Compliant
    val l2 = mapOf(1 to "one", 2 to "two", 3 to "five")
    val a = l2[123]!! // Noncompliant {{`Map` values should be accessed safely. Using the non-null assertion operator here can throw a NullPointerException.}}
//          ^^^^^^^^^

    fun test() {
        val m = listOf("1", "2", "3")
        m[3]!! // Compliant
        m.get(3) // Compliant
        val l = mapOf(1 to "one", 2 to "two", 3 to "five")
        l.get(123)!! // Noncompliant {{`Map` values should be accessed safely. Using the non-null assertion operator here can throw a NullPointerException.}}
//      ^^^^^^^^^^^^
        l[123]!! // Noncompliant

        val a = l.get(123) ?: ""

        l.get(123)!!.get(1) // Noncompliant
    }

    fun test2() {
        val l = mapOf(1 to "one", 2 to "two", 3 to "five")
        l.get(123) // Compliant, returns nullable
        l[123] // Compliant, returns nullable
        l.getValue(123) // Compliant, throws NoSuchElementException
        l.getOrElse(123) { "empty" } // Compliant, has default
        l.getOrDefault(123, "empty") // Compliant, has default
        l[123] ?: "empty" // Compliant, has default
    }

    fun test3(map: Map<Int, String>, mutableMap: MutableMap<Int, String>) {
        map.get(123)!! // Noncompliant
        map[0]!! // Noncompliant

        mutableMap.get(1)!! // Noncompliant
        mutableMap.get(1) // Compliant
        mutableMap[1]!! // Noncompliant
        mutableMap[1] // Compliant
    }
}
