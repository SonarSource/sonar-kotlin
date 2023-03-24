package checks

private class Void

private class MyIII {
    fun voidFunction1(): Void = TODO() // Compliant, custom void
    fun voidFunction11(): Void? = null // Compliant, custom void
    fun voidFunction2(): java.lang.Void  = TODO() // Noncompliant
}