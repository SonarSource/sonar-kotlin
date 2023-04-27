package checks

const val BOOL = true

class SimplifiedPreconditionsCheckSample {

    fun f(i: Int) = i < 5

    fun sample(x: Any?, state: Any?) {

        check(x != null) // Noncompliant {{Replace this `check` function call with `checkNotNull(x)`.}}
//      ^^^^^

        check(true)
        check(x.equals(x))

        var msg = "state is null"
        if (state == null) throw IllegalStateException(msg) // Noncompliant {{Replace this `if` expression with `checkNotNull(state) { msg }`.}}
        if (state == null) throw IllegalStateException("state is null") // Noncompliant {{Replace this `if` expression with `checkNotNull(state) { "state is null" }`.}}
        if (state == null) throw IllegalStateException(msg, IllegalCallerException())
        if (state == null) throw IllegalStateException(IllegalCallerException())

        if (state == -1) throw IllegalStateException("state is -1") // Noncompliant {{Replace this `if` expression with `check(state != -1) { "state is -1" }`.}}
//      ^^
        else if (state == -2) { throw IllegalStateException("state is -2") } // Noncompliant {{Replace this `if` expression with `check(state != -2) { "state is -2" }`.}}
//           ^^
        else throw IllegalStateException("message") // Noncompliant {{Replace this `throw` expression with `error("message")`.}}
//           ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

        if (f(55)) throw IllegalStateException("message") // Noncompliant {{Replace this `if` expression with `check(!f(55)) { "message" }`.}}

        if (!f(55)) throw IllegalStateException("message") // Noncompliant {{Replace this `if` expression with `check(f(55)) { "message" }`.}}

        if (f(55)) throw IllegalCallerException() // Compliant, different exception
        else { throw IllegalStateException("message") } // Noncompliant {{Replace this `throw` expression with `error("message")`.}}

        if (state == -3) {
            println()
            throw IllegalStateException("compliant ise")
        }

        when (state) {
            0..10 -> check(state == null)
            11..1000 -> check(state != null) // Noncompliant {{Replace this `check` function call with `checkNotNull(state)`.}}
//                      ^^^^^
            else -> throw IllegalStateException("state is $state") // Noncompliant {{Replace this `throw` expression with `error("state is $state")`.}}
//                  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        }

        when {
            state == null -> throw IllegalStateException("when check") // Compliant
            else -> { throw IllegalStateException() } // Noncompliant {{Replace this `throw` expression with `error("")`.}}
        }

        require(null != x) // Noncompliant {{Replace this `require` function call with `requireNotNull(x)`.}}
//      ^^^^^^^

        if (x == null) throw IllegalArgumentException() // Noncompliant {{Replace this `if` expression with `requireNotNull(x)`.}}
//      ^^
        if (x != -1) { throw IllegalArgumentException() } // Noncompliant {{Replace this `if` expression with `require(x == -1)`.}}
        if (x != -1) throw IllegalArgumentException("x is -1") // Noncompliant {{Replace this `if` expression with `require(x == -1) { "x is -1" }`.}}
        else { throw IllegalArgumentException("message") } // Compliant

        if (x is Int) throw IllegalArgumentException("expected type") // Noncompliant {{Replace this `if` expression with `require(x !is Int) { "expected type" }`.}}
        if (x !is Int) throw IllegalArgumentException("expected type") // Noncompliant {{Replace this `if` expression with `require(x is Int) { "expected type" }`.}}

        if (!(x is Int)) throw IllegalArgumentException("unexpected type") // Noncompliant {{Replace this `if` expression with `require((x is Int)) { "unexpected type" }`.}}

        var b = false
        if (b) throw IllegalArgumentException() // Noncompliant {{Replace this `if` expression with `require(!b)`.}}
        if (!b) throw IllegalArgumentException() // Noncompliant {{Replace this `if` expression with `require(b)`.}}
        if (BOOL) { throw IllegalArgumentException() } // Noncompliant {{Replace this `if` expression with `require(!BOOL)`.}}

        if (state == -1 || state == 2) throw IllegalArgumentException() // Noncompliant {{Replace this `if` expression with `require(!(state == -1 || state == 2))`.}}
        if (state == -1 && state != 2) throw IllegalArgumentException() // Noncompliant {{Replace this `if` expression with `require(!(state == -1 && state != 2))`.}}
        if (!b && state != 2) throw IllegalArgumentException() // Noncompliant {{Replace this `if` expression with `require(!(!b && state != 2))`.}}

        var y = 5
        if (y < 0) throw IllegalArgumentException() // Noncompliant {{Replace this `if` expression with `require(y >= 0)`.}}
        if (y <= 0) throw IllegalArgumentException() // Noncompliant {{Replace this `if` expression with `require(y > 0)`.}}
        if (y > 0) throw IllegalArgumentException() // Noncompliant {{Replace this `if` expression with `require(y <= 0)`.}}
        if (y >= 0) throw IllegalArgumentException() // Noncompliant {{Replace this `if` expression with `require(y < 0)`.}}

        if (true) throw IllegalArgumentException() // Noncompliant {{Replace this `if` expression with `require(false)`.}}
        if (false) throw IllegalArgumentException() // Noncompliant {{Replace this `if` expression with `require(true)`.}}

        val collection = listOf<String>()
        if (collection.isNotEmpty()) throw IllegalArgumentException() // Noncompliant {{Replace this `if` expression with `require(!(collection.isNotEmpty()))`.}}

        if (+"") throw IllegalArgumentException() // Noncompliant {{Replace this `if` expression with `require(!(+""))`.}}
    }

}

// fictitious operator override to cover PrefixExpression
operator fun String.unaryPlus(): Boolean = true


