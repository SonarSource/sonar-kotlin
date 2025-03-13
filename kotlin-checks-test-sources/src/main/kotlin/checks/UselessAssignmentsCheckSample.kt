package checks

class UselessAssignmentsCheckSample {

    private fun f() {
        var i = 0 // Noncompliant {{Remove this useless initializer.}}
//              ^

        i = 1 // Noncompliant {{The value assigned here is never used.}}
//      ^^^^^
        i = 2 + 5 // Noncompliant
        i = 3


        val j = i++ // Noncompliant
//              ^^^

        var k = 0 // Noncompliant {{Remove this variable, which is assigned but never accessed.}}
//          ^
        k = 1 // Noncompliant

        println(j)
    }

    private fun prefix_operator() {
        var a = 0
        ++a // Noncompliant

        var b = 0
        ++b
        println(b)

        var c = 0
        println(++c)

        var d = 0
        println(if (true) ++d else ++d)
    }
}
