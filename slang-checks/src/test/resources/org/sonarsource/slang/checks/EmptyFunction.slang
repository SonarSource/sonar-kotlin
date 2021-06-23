fun noBody();

fun notEmpty() { bar() }

// Noncompliant@+1
fun empty() {}
//          ^^

fun containingOnlyAComment() { /* comment */ }

fun emptyWithEndOfLineComment1() {} // end of line comment

fun emptyWithEndOfLineComment2() { } /* comment */

fun emptyWithEndOfLineCommentOnMultipleLine() { } /* comment
*/

fun emptyOnSeveralLine() {
} // comment

// Noncompliant@+1
fun empty() {
}

class A {
  // Compliant, constructor
  fun constructor() {
  }
}
