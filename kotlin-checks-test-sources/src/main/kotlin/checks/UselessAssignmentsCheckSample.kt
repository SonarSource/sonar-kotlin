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
}
