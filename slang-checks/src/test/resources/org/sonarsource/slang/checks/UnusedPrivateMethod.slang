class A {
  private fun unusedPrivate() {} // Noncompliant

  public fun unusedPrivate(int a) {}

  private fun unusedPrivate(int a) {} // Noncompliant

  private override fun unusedPrivateOverride(int a) {} // OK - might be forced to override the method here

  private fun usedPrivate(int a) {}

  fun unusedNoModifier(int a) {
    usedPrivate();
  }

  public fun unusedPublic(int a) {}
}

class B {
    private fun f1() {}

    fun g(int f1) {} // FN of current approach, "f1" is used as identifier elsewhere in the class

    private fun f2() {}

    fun h(C c) { native[] {  [c;][f2();] } } // FN of current approach, another class has same identifier for a method call "f2" (c.f2();)

    private fun writeObject() {} // Noncompliant
}

class C {

    private fun constructor() {}  // Compliant to prevent FP, constructor call semantic is not yet supported

    private fun unusedPrivate1() {}  // Noncompliant

    private fun unusedPrivate2() {}  // FN of current approach, "unusedPrivate2" identifier is used in inner class

    private fun unusedPrivate4() {}

    private fun f1() {
      f1();
      usedInnerMethod1();
      usedInnerMethod2();
      unusedPrivate4();
    }

    class D {
        private fun unusedPrivate2() {}

        private fun unusedPrivate3() {}  // Noncompliant

        private fun unusedPrivate4() {} // FN of current approach, "unusedPrivate4" identifier is used in outer class

        private fun f1() {}  // FN of current approach, "f1" is used in outer class

        private fun usedInnerMethod1() {}

        private fun f2() {
          f2();
          unusedPrivate2();
        }

        class E {
            private fun unusedPrivate5() {}  // Noncompliant

            private fun usedInnerMethod2() {}
        }
    }
}
