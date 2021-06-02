package checks

class BooleanLiteralCheckSample {
    fun f() {
        var x = true
        var foo = true
        var y = true
        var z = true
        var bar = true
        var a = true
        var b = true

        if (x) {

        }

        x == true                                   // OK - as for now without semantic we do not know if x is nullable or a primitive
        x == false                                  // OK - as for now without semantic we do not know if x is nullable or a primitive
        x != true                                   // OK - as for now without semantic we do not know if x is nullable or a primitive
        x != false                                  // OK - as for now without semantic we do not know if x is nullable or a primitive
        true == x                                   // OK - as for now without semantic we do not know if x is nullable or a primitive
        false == x                                  // OK - as for now without semantic we do not know if x is nullable or a primitive
        true != x                                   // OK - as for now without semantic we do not know if x is nullable or a primitive
        false != x                                  // OK - as for now without semantic we do not know if x is nullable or a primitive
        !true                                       // Noncompliant {{Remove the unnecessary Boolean literal.}}

        !false                                      // Noncompliant {{Remove the unnecessary Boolean literal.}}
        false && foo                                // Noncompliant {{Remove the unnecessary Boolean literal.}}
        x || true                                   // Noncompliant {{Remove the unnecessary Boolean literal.}}
        x || ((true))                               // Noncompliant {{Remove the unnecessary Boolean literal.}}

        !x                                          // OK
        x || foo                                    // OK
        x == y                                      // OK
        z != x                                      // OK

        x = if (foo) y else false                   // Noncompliant {{Remove the unnecessary Boolean literal.}}
        x = if (foo) y else true                    // Noncompliant {{Remove the unnecessary Boolean literal.}}
        x = if (foo) true else y                    // Noncompliant {{Remove the unnecessary Boolean literal.}}
        x = if (foo) false else y                   // Noncompliant {{Remove the unnecessary Boolean literal.}}


        x = if (foo) x else y                       // OK

        var elem = ""
        if (elem is String) elem.contains("dfgdv") else false // Noncompliant {{Remove the unnecessary Boolean literal.}}

        x = if (foo) false
        else {
            doSomething()
            y
        }

        x = if (foo) false
        else if (bar) {
            doSomething()
            y
        } else true

        x = if (foo) {
            doSomething()
            y
        } else true

        x = if (a) true // Noncompliant
        else false

        x = if (a) true
        else if (b) false
        else false

        x =
            if (b)
                if (a) true // Noncompliant
                else false
            else a
    }

    private fun doSomething() {
        TODO("Not yet implemented")
    }
}
