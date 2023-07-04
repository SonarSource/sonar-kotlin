  println("Hello!") // Compliant
  pruntln("Hello!") // Noncompliant {{Use "println" instead of "pruntln"}}
//^^^^^^^
  println("Hello!") // Compliant

  fun pruntln(value: String) {
      println(value)
  }
