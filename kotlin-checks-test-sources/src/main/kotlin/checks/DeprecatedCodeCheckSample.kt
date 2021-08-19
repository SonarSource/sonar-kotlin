package checks

@Deprecated("") 
class DeprecatedCodeCheckSample { // Noncompliant {{Do not forget to remove this deprecated code someday.}}
//    ^^^^^^^^^^^^^^^^^^^^^^^^^
  
  @Deprecated("")
  constructor(s: String) { // Noncompliant
//^^^^^^^^^^^    
  }
  
  @Deprecated("")
  val prop: String = "" // Noncompliant
//    ^^^^
  
  var prop2: String = ""
  @Deprecated("") set // Noncompliant
//                ^^^    
  @Deprecated("") get // Noncompliant
//                ^^^    

}

@Deprecated("")
fun deprecated() { // Noncompliant
//  ^^^^^^^^^^  
  
}

@Deprecated("")
annotation class Deprecated2 // Noncompliant

class OK

@Deprecated("")
typealias KtString = String // Noncompliant
//        ^^^^^^^^

