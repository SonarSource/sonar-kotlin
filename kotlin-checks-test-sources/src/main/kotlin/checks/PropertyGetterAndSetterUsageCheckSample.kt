package checks

open class PropertyGetterAndSetterUsageCheckSample {

    private class PrivateClassGetterAndSetter {
        private var size: Int = 42
//                  ^^^^> {{Property "size"}}
        fun getSize(): Int = size // Noncompliant {{Convert this getter to a "get()" on the property "size".}}
//          ^^^^^^^
        fun setSize(size: Int) {
//          ^^^^^^^< {{Setter to convert to a "set(value)"}}
            this.size = size
        }
        fun setSize(other: String) { } // Compliant, argument type is "String" instead of "Int"
    }
    private class PrivateClassOnlyGetter {
        private val name: String = "42"
//                  ^^^^> {{Property "name"}}
        fun getName(): String = name // Noncompliant {{Convert this getter to a "get()" on the property "name".}}
//          ^^^^^^^
    }
    private class PrivateClassOnlySetter {
        private var id: String = ""
        //          ^^> {{Property "id"}}
        fun setId(id: String) { // Noncompliant {{Convert this setter to a "set(value)" on the property "id".}}
//          ^^^^^
            this.id = id
        }
    }
    private class PrivateClassBooleanGetter {
        private val active: Boolean = true
//                  ^^^^^^> {{Property "active"}}
        fun isActive(): Boolean = active // Noncompliant {{Convert this getter to a "get()" on the property "active".}}
//          ^^^^^^^^
    }
    private open class PrivateClassWithoutIssues(
        private val constructorPrivateVal: String,
        private var constructorPrivateVar: String
    ) {
        private var value1: String = ""
        private var value2: String = ""
        private var value3: String? = null
        private val value4: Boolean = true
        private var value5: String = ""
        private var value6: Boolean? = null
        private val value7: Boolean
            get() = true
        private var _value8: Boolean = true
        private var value8: Boolean
            get() = _value8
            set(value) { _value8 = value }
        protected var value9: Boolean = true
        private var away: String = ""
        private var TER: String = ""
        fun getConstructorPrivateVal(): String = constructorPrivateVal // Compliant
        fun getConstructorPrivateVar(): String = constructorPrivateVar // Compliant
        fun setConstructorPrivateVar(value: String ) { constructorPrivateVar = value } // Compliant

        fun getaway(): String = "" // Compliant, "getaway" instead of "getAway"
        fun getTer(): String = "" // Compliant, "getTer" instead of "getTER"
        fun setTer(value: String) { } // Compliant, "getTer" instead of "getTER"
        fun getValue1(): Int = 0 // Compliant, return "Int" instead of "String"
        fun setValue1(value: Int) { } // Compliant, argument type is "Int" instead of "String"
        fun setValue1(vararg value: String) { } // Compliant, argument type is "vararg String" instead of "String"
        fun setValue1(value1: String, value2: String) { } // Compliant, too many arguments
        fun getValue2(): String? = null // Compliant, return "String?" instead of "String"
        fun setValue2(value: String?) { } // Compliant, argument type is "String?" instead of "String"
        fun getValue3(): String = "" // Compliant, return "String" instead of "String?"
        fun setValue3(value: String) { } // Compliant, argument type is "String" instead of "String?"

        fun isValue4(): Boolean = false // Compliant, ambiguous "isValue4" and "getValue4"
        fun getValue4(): Boolean = false // Compliant, ambiguous "isValue4" and "getValue4"

        fun getValue5(arg: String): String = "" // Compliant, getter with some arguments
        fun isValue5(): String = "" // Compliant, getter prefixed with "is" does not match a "Boolean" property
        fun setValue5(value: String): String = "" // Compliant, setter does not return "Unit"
        fun isValue6(): Boolean? = null // Compliant, getter prefixed with "is" does not match a "Boolean" property

        fun isValue7(): Boolean = false // Compliant, property already has getter
        fun isValue8(): Boolean = false // Compliant, property already has getter and setter
        fun isValue9(): Boolean = false // Compliant, property is not private but protected
    }
    private class PrivateClassWithoutGetterAndSetter {
        fun foo() {}
    }

    private class PrivateClassWithoutBody

    protected open class ProtectedClass {
        private val value: Int = 42
        fun getValue(): Int = value // Noncompliant
    }
    internal class InternalClass {
        private val value: Int = 42
        fun getValue(): Int = value // Noncompliant
    }
    class NoIssueOnImplicitPublicClass {
        private val value: Int = 42
        fun getValue(): Int = value // Compliant
    }
    public class NoIssueOnExplicitPublicClass {
        private val value: Int = 42
        fun getValue(): Int = value // Compliant
    }
}
