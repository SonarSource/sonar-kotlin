package checks

class CollectionShouldBeImmutableCheckSample {
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

    fun sumListComplinat(list: List<Int>): Int { // Compliant
        return list.reduce { acc, it -> acc + it}
    }

    fun MutableList<Int>.sumNoncompliant(): Int { // Noncompliant
        return reduce { acc, it -> acc + it}
    }

    fun List<Int>.sumCompliant(): Int { // Compliant
        return reduce { acc, it -> acc + it}
    }
}
