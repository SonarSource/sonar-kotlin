package checks

open class PropertyGetterAndSetterUsageCheckSamplePublic {
    class ImplicitPublicClass {
        private val value: Int = 42
        fun getValue(): Int = value // Noncompliant
    }
    public class ExplicitPublicClass {
        private val value: Int = 42
        fun getValue(): Int = value // Noncompliant
    }
    protected open class ProtectedClass {
        private val value: Int = 42
        fun getValue(): Int = value // Noncompliant
    }
    internal class InternalClass {
        private val value: Int = 42
        fun getValue(): Int = value // Noncompliant
    }
    private class PrivateClassGetterAndSetter {
        private var size: Int = 42
        fun getSize(): Int = size // Noncompliant
    }
}
