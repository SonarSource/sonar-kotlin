package checks

class TooDeeplyNestedStatementsCheckSample {
    fun test() {
        // Noncompliant@+2 {{Refactor this code to not nest more than 1 control flow statements.}}
        if (false)
            if (false)
//          ^^
                if (false)
                    println()

        if (false)
            println()
        else if (false)
            println()

        // Noncompliant@+4
        if (false)
            println()
        else if (false) {
            if (false)
//          ^^
                println()
        }

        // Noncompliant@+2
        if (false)
            when (0) {
//          ^^^^
            }

        // Noncompliant@+2
        try {
            try {
//          ^^^
            } finally {
            }
        } finally {
        }

        // Noncompliant@+2
        for (i in 0..1)
            for (j in 0..1)
//          ^^^
                println()

        if (false)
            return if (false) println() else println()
    }
}
