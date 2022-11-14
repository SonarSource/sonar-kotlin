package checks

class EqualsOverridenWithHashCodeCheckSample {


    override fun equals(other: Any?): Boolean { // Noncompliant {{This class overrides "equals()" and should therefore also override "hashCode()".}}
        return super.equals(other)
    }

    fun test() {
        println("test")
    }

    class Compliant {
        override fun equals(other: Any?): Boolean {
            return super.equals(other)
        }

        override fun hashCode(): Int {
            return super.hashCode()
        }
    }


    class NonCompliant {
        override fun hashCode(): Int { // Noncompliant
            return super.hashCode()
        }

        class Inner {
            override fun equals(other: Any?): Boolean { // Noncompliant
                return super.equals(other)
            }

            fun hashCode(x: Int): Int {
                return 0
            }
        }

    }

    data class DC(val x: String) {
        override fun hashCode(): Int { // Noncompliant
            return super.hashCode()
        }
    }

    data class DC2(val x: String) {
        override fun equals(other: Any?): Boolean { // Noncompliant
            return super.equals(other)
        }
    }

    data class DCCompliant(val x: String) {
        override fun equals(other: Any?): Boolean {
            return super.equals(other)
        }

        override fun hashCode(): Int {
            return super.hashCode()
        }
    }


}

abstract class AmbiguousParent {
    abstract override fun equals(other: Any?): Boolean
}

abstract class AmbiguousParent4 {
    abstract override fun hashCode(): Int
}

abstract class AmbiguousParent2 {
    abstract override fun hashCode(): Int

    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
}

abstract class AmbiguousParent3 {

    override fun hashCode(): Int { // Noncompliant
        return super.hashCode()
    }
    fun equals(other: String?): Boolean {
        return super.equals(other)
    }
}
