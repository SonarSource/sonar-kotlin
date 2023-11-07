package checks

class UselessNullCheckCheckSample {
    fun foo() {
        val s: String = ""

        if (s == null) {} // Noncompliant {{Remove this useless null check, it always fails.}}
//          ^^^^^^^^^

        if (s != null) {} // Noncompliant {{Remove this useless non-null check, it always succeeds.}}
        s?.doSomething() // Noncompliant {{Remove this useless null-safe access, it always succeeds.}}
//       ^^
        fun foo(s: Any): String {
            s ?: return "" // Noncompliant {{Remove this useless elvis operation, it always succeeds.}}
//            ^^
            return s.toString()
        }
        requireNotNull(s) // Noncompliant {{Remove this useless non-null check, it always succeeds.}}
//      ^^^^^^^^^^^^^^^^^

        checkNotNull(s) // Noncompliant {{Remove this useless non-null check, it always succeeds.}}
        s!!.doSomething() // Noncompliant {{Remove this useless non-null assertion (!!), it always succeeds.}}
//       ^^
        null!! // Noncompliant {{Remove this useless non-null assertion (!!), it always fails.}}
        null?.doSomething() // Noncompliant {{Remove this useless null-safe access, it always fails.}}
        null ?: doSomething() // Noncompliant {{Remove this useless elvis operation, it always fails.}}
        doSomething() ?: null // Noncompliant {{Remove this useless elvis operation, it always succeeds.}}
    }

    private fun Any?.doSomething() {}
}
