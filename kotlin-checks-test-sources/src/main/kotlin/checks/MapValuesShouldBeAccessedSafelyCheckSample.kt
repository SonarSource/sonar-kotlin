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

    fun test4(customMap: CustomMap) {
        customMap.get(1)!! // Noncompliant
        customMap[2]!! // Noncompliant
        customMap.get(2) // Compliant
        customMap[2] // Compliant
        customMap.getValue(123) // Compliant
    }

    fun test5(hashMap: HashMap<Int, Int>) {
        hashMap.get(1)!! // Noncompliant
        hashMap[2]!! // Noncompliant
        hashMap.get(2) // Compliant
        hashMap[2] // Compliant
        hashMap.getValue(123) // Compliant
    }
}

class CustomMap : Map<Int, Int> {
    override val entries: Set<Map.Entry<Int, Int>>
        get() = TODO("Not yet implemented")
    override val keys: Set<Int>
        get() = TODO("Not yet implemented")
    override val size: Int
        get() = TODO("Not yet implemented")
    override val values: Collection<Int>
        get() = TODO("Not yet implemented")

    override fun isEmpty(): Boolean {
        TODO("Not yet implemented")
    }

    override fun get(key: Int): Int? {
        TODO("Not yet implemented")
    }

    override fun containsValue(value: Int): Boolean {
        TODO("Not yet implemented")
    }

    override fun containsKey(key: Int): Boolean {
        TODO("Not yet implemented")
    }

}