package checks

class DuplicateBranchCheckSample {
    fun f(x: Boolean, y: Boolean, xxx: Int) {
        if (x) {
            foo()
            foo()
        }

        if (x) {
            foo()
            foo()
        } else {
            bar()
            bar()
        }

        if (x) { // handled by S3923
            foo()
            foo()
        } else {
            foo()
            foo()
        }

        if (x) {
            foo()
            foo()
        } else if (y) { // Noncompliant {{This branch's code block is the same as the block for the branch on line 26.}}
            foo()
            foo()
        } else {
            bar()
            bar()
        }

        if (x) {
            foo()
            foo()
        } else if (y) {
            bar()
            bar()
        } else { // Noncompliant
            bar()
            bar()
        }

        if (x) {

        } else if (y) {

        } else {
            bar() 
            bar()
        }

        if (x) {
            foo() 
            foo()
        } else if (y) { // Noncompliant
            foo() 
            foo()
        } else {
            bar() 
            bar()
        }

        if (x)
            foo()
        else if (y)
            foo()
        else
            bar()
        

        if (x) {
            foo()
            + bar()
        }
        else if (y) { // Noncompliant
            foo()
                   + bar()
        }
        else
        bar()
        

        when(xxx) {
            1 -> foo() + bar()
            2 -> foo() + baz()
            3 -> { // Compliant
                foo() +bar()
            }
        }

        when(xxx) {
            1 -> foo() + bar()
            2 -> foo() + baz()
            3 ->
                foo() + bar() // Compliant
        }

        when(xxx) {
            1 -> ""
            2 -> foo()+ baz()
            3 -> "abc"
        }

        when(xxx) {
            1 -> { "" }
            2 -> { foo()+ baz() }
            3 -> { "abc" }
        }

        when(xxx) {
            1 -> {
                foo()+ baz()
            }
            2 -> {
                foo()+ baz()
            }
            3 -> {
                foo()+ baz()
            }
        }

        when(xxx) {
            1 -> """{
                foo()+ baz()
            }"""
            // Noncompliant@+1
            3 -> """{
                foo()+ baz()
            }"""
            // Noncompliant@+1
            2 -> """{
                foo()+ baz()
            }"""
        }

    }

    private fun baz(): Int {
        return 1
    }

    private fun bar() : Int {
        return 0
    }

    private fun foo(): Int {
        return 3
    }
}
