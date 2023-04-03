package checks

class IndexedAccessCheckSample {

    fun withoutIndexedAccessors(list: MutableList<Int>, map: MutableMap<String, Int>, grid: Grid) {
        list.get(1) // Noncompliant {{Replace function call with indexed accessor.}}
//           ^^^^^^
        list.set(1, 42) // Noncompliant {{Replace function call with indexed accessor.}}
//           ^^^^^^^^^^
        map.get("b") // Noncompliant {{Replace function call with indexed accessor.}}
        map.set("b", 42) // Noncompliant {{Replace function call with indexed accessor.}}
//          ^^^^^^^^^^^^
        grid.get(1, 2) // Noncompliant {{Replace function call with indexed accessor.}}
        grid.set(1, 2, 42) // Noncompliant {{Replace function call with indexed accessor.}}
    }

    fun withIndexedAccessors(lisp: Lisp<Int>, list: MutableList<Int>, map: MutableMap<String, Int>, grid: Grid) {
        lisp.get(1) // Compliant, not an operator
        list[1] // Compliant
        list[1] = 42 // Compliant
        map["b"] // Compliant
        map["b"] = 42 // Compliant
        grid[1, 2] // Compliant
        grid[1, 2] = 42 // Compliant
        list.getOrNull(2) // Complaint, not an operator
        list.getOrElse(3) {42} // Complaint, not an operator
        map.getValue("a") // Complaint, not an operator
        map.getOrElse("c") {42} // Complaint, not an operator
    }
}

interface Grid {
    operator fun get(row: Int, column: Int): Int
    operator fun set(row: Int, column: Int, value: Int)
}

interface Lisp<T> {
    /*operator*/ fun get(index: Int)
}
