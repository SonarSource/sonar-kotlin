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
//                  ^^^
  var prop3: String = ""
    @Deprecated("") get // Noncompliant
//                  ^^^

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

@Deprecated("")
private operator fun KtString.minus(s: String) = this + s // Noncompliant

class DeprecatedPrimaryConstructor @Deprecated("") constructor() { // Noncompliant
//                                                 ^^^^^^^^^^^  
}

var anonymousFunction = @Deprecated("") fun() { // Noncompliant
//                      ^^^^^^^^^^^^^^^ 
}


var anonymousClass = @Deprecated("") object : Any() { // Noncompliant
//                   ^^^^^^^^^^^^^^^ 

}
