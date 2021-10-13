package sources.kotlin

class S2123 {
    fun foo(): Int {
        var i = 1
        return i++ // Noncompliant
    }
}
