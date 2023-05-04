package checks

class ImplicitParameterInLambdaCheckSample {

    fun sample() {
        listOf(1, 2, 3).forEach { it: Int -> it.and(6) } // Compliant, type is specified explicitly
        listOf(1, 2, 3).forEach {
            it ->  // Noncompliant {{Remove this `it` parameter declaration or give this lambda parameter a meaningful name.}}
            it.and(6)
        }
        listOf(1, 2, 3).forEach { it.and(6) } // Compliant
        listOf(1, 2, 3).forEach { x -> x.and(6) } // Compliant

        listOf(1, 2, 3).forEach({ it: Int -> it.and(6) }) // Compliant, type is specified explicitly
        listOf(1, 2, 3).forEach({ it -> it.and(6) }) // Noncompliant {{Remove this `it` parameter declaration or give this lambda parameter a meaningful name.}}
        listOf(1, 2, 3).forEach({ it.and(6) }) // Compliant

        listOf(1, 2, 3).forEach(action = { it: Int -> it.and(6) }) // Compliant, type is specified explicitly
        listOf(1, 2, 3).forEach(action = {
            it ->  // Noncompliant {{Remove this `it` parameter declaration or give this lambda parameter a meaningful name.}}
            it.and(6)
        })
        listOf(1, 2, 3).forEach(action = { it.and(6) }) // Compliant

        val l1: (Int) -> Int = { it -> it + 5 } // Noncompliant {{Remove this `it` parameter declaration or give this lambda parameter a meaningful name.}}
//                               ^^
        val l2: (Int) -> Int = { it: Int -> it + 5 } // Noncompliant {{Remove this `it` parameter declaration or give this lambda parameter a meaningful name.}}
//                               ^^^^^^^
        val l3: (Int) -> Int = { it + 5 } // Compliant
        val l4 = { it: Int -> it + 5 } // Compliant, need to know the type

        val l5 = { -> 5 } // Compliant
        val l6 = { x: Int -> x + 5 } // Compliant
        val l7 = { x: Int, y: Int -> x + y + 5 } // Compliant

        incrementer { it: Int -> it.inc() } // Compliant, type is specified explicitly
        incrementer { it -> it.inc() } // Noncompliant {{Remove this `it` parameter declaration or give this lambda parameter a meaningful name.}}
//                    ^^
        incrementer { it.inc() } // Compliant

        adder { x: Int, y: Int -> x + y } // Compliant
        adder { x, y -> x + y } // Compliant
    }

    class Incrementer {
        val INCREMENTER: (Int) -> Int = { it -> it.inc() } // Noncompliant {{Remove this `it` parameter declaration or give this lambda parameter a meaningful name.}}
    }

    private fun incrementer(incrementFun: (x: Int) -> Int) = incrementFun(5)
    private fun adder(addFun: (x: Int, y: Int) -> Int) = addFun(5, 6)
}
