package checks

class CollectionShouldBeImmutableCheckSampleNonCompiling {

    actual fun actualFun(list: MutableList<Int>): Unit // compliant
    expect fun expectFun(list: MutableList<Int>): Unit // compliant

    fun qualifiedStarange() {
        val list = mutableListOf<Int>()
        list.(add(1))
    }
}