package checks

class ElseIfWithoutElseCheckSample {

    fun test(c: Boolean) {
        if(c)
            else ""

        if (c)
            if (c)
                return
            else if (c) // Noncompliant {{Add the missing "else" clause.}}
                println()

        if (c)
            println()
        else if (c)
            println()
        else
            println()

        for (i in 0..1) {
            if (c)
                break
            else if (c)
                continue
            else if (c)
                return
            else if (c)
                throw RuntimeException()
        }

        emptyList<Int>().forEach {
            if (it > 0)
                return@forEach
            else if (it < 0)
                return@forEach
        }
    }

}
