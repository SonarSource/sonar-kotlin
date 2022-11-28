package checks

class CollectionCallingItselfCheckSample {

    fun nonCompliant() {
        val strings: MutableList<String> = ArrayList()
        strings.add("Hello")
        strings.containsAll(strings) // Noncompliant
        strings.addAll(strings) // Noncompliant
        strings.removeAll(strings) // Noncompliant
        strings.retainAll(strings) // Noncompliant {{Collections should not be passed as arguments to their own methods.}}
//                        ^^^^^^^

        strings.addAll(strings as Iterable<String>) // Noncompliant
        strings.containsAll<Any>(strings) // Noncompliant
        strings.removeAll<Any>(strings) // Noncompliant
        strings.retainAll<Any>(strings) // Noncompliant

        val anys = mutableListOf<Any>()
        anys.add(anys) // Noncompliant
        anys.fill(anys) // Noncompliant
    }

    fun compliant(newList: MutableList<String>) {
        val strings: MutableList<String> = ArrayList()
        strings.add("Hello")
        strings.containsAll(newList) // Compliant
        strings.addAll(newList) // Compliant
        strings.removeAll(newList) // Compliant
        strings.retainAll(newList) // Compliant

        strings.addAll(newList as Iterable<String>) // Compliant
        strings.containsAll<Any>(newList) // Compliant
        strings.removeAll<Any>(newList) // Compliant
        strings.retainAll<Any>(newList) // Compliant

        val anys = mutableListOf<Any>()
        anys.add(newList) // Compliant
        anys.fill(newList) // Compliant
    }

    fun compliant1() {
        val strings: MutableList<String>
        strings = ArrayList()
        strings.add("Hello")

        val strings1: MutableList<String>
        strings1 = ArrayList()
        strings1.add("Test")
        strings1.addAll(strings)

        strings.containsAll(strings1) // Compliant fn we don't track transitive operations
        strings.addAll(strings1) // Compliant
        strings.removeAll(strings1) // Compliant
        strings.retainAll(strings1) // Compliant

        strings.addAll(strings1 as Iterable<String>) // Compliant
        strings.containsAll<Any>(strings1) // Compliant
        strings.removeAll<Any>(strings1) // Compliant
        strings.retainAll<Any>(strings1) // Compliant

        val anys = mutableListOf<Any>()
        anys.add(strings1) // Compliant
        anys.fill(strings1) // Compliant
    }

}
