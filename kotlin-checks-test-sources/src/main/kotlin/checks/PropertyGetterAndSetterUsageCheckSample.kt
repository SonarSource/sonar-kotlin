package checks

import java.util.Date

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

    open class GetterAndSetterVisibilityCompatibility {
        // Kotlin's properties only support setter visibility <= getter visibility
        // So the rule should not raise issue when accessors visibility is not compatible with Kotlin's properties

        // ______________________________________________
        // 1) Setter with visibility <= getter visibility

        // public getter / public setter
        private var f1: Int = 0
        fun getF1(): Int = f1 // Noncompliant
        fun setF1(value: Int) { f1 = value }

        // public getter / internal setter
        private var f2: Int = 0
        fun getF2(): Int = f2 // Noncompliant
        internal fun setF2(value: Int) { f2 = value }

        // public getter / protected setter
        private var f3: Int = 0
        fun getF3(): Int = f3 // Noncompliant
        protected fun setF3(value: Int) { f3 = value }

        // public getter / private setter
        private var f4: Int = 0
        fun getF4(): Int = f4 // Noncompliant
        private fun setF4(value: Int) { f4 = value }

        // internal getter / internal setter
        private var f5: Int = 0
        internal fun getF5(): Int = f5 // Noncompliant
        internal fun setF5(value: Int) { f5 = value }

        // internal getter / private setter
        private var f6: Int = 0
        internal fun getF6(): Int = f6 // Noncompliant
        private fun setF6(value: Int) { f6 = value }

        // protected getter / protected setter
        private var f7: Int = 0
        protected fun getF7(): Int = f7 // Noncompliant
        protected fun setF7(value: Int) { f7 = value }

        // protected getter / private setter
        private var f8: Int = 0
        protected fun getF8(): Int = f8 // Noncompliant
        private fun setF8(value: Int) { f8 = value }

        // private getter / private setter
        private var f9: Int = 0
        private fun getF9(): Int = f9 // Noncompliant
        private fun setF9(value: Int) { f9 = value }

        // ______________________________________________
        // 2) Setter with visibility > getter visibility

        // internal getter / public setter
        private var f10: Int = 0
        internal fun getF10(): Int = f10 // Compliant
        public fun setF10(value: Int) { f10 = value }

        // internal getter / protected setter
        private var f11: Int = 0
        internal fun getF11(): Int = f11 // Compliant
        protected fun setF11(value: Int) { f11 = value }

        // protected getter / public setter
        private var f12: Int = 0
        protected fun getF12(): Int = f12 // Compliant
        public fun setF12(value: Int) { f12 = value }

        // protected getter / internal setter
        private var f13: Int = 0
        protected fun getF13(): Int = f13 // Compliant
        internal fun setF13(value: Int) { f13 = value }

        // private getter / public setter
        private var f14: Int = 0
        private fun getF14(): Int = f14 // Compliant
        public fun setF14(value: Int) { f14 = value }

        // private getter / internal setter
        private var f15: Int = 0
        private fun getF15(): Int = f15 // Compliant
        internal fun setF15(value: Int) { f15 = value }

        // private getter / protected setter
        private var f16: Int = 0
        private fun getF16(): Int = f16 // Compliant
        protected fun setF16(value: Int) { f16 = value }

        // missing getter / public setter
        private var f20: Int = 0
        fun setF20(value: Int) { f20 = value } // Compliant

        // missing getter / internal setter
        private var f21: Int = 0
        internal fun setF21(value: Int) { f21 = value } // Compliant

        // missing getter / protected setter
        private var f22: Int = 0
        protected fun setF22(value: Int) { f22 = value } // Compliant

        // missing getter / private setter
        private var f23: Int = 0
        private fun setF23(value: Int) { f23 = value } // Compliant
    }

    abstract class ClassOverridingAccessors : Date() {
        private var time: Long = 0
        private var foo: Long = 0

        override fun getTime(): Long = time // Compliant, getter and setter override a function

        override fun setTime(value: Long) {
            super.setTime(time)
        }

        abstract fun setFoo(value: Long)
    }

    class ClassOverridingOnlySetter : ClassOverridingAccessors() {
        private var foo: Long = 0
        fun getFoo(): Long = foo // Compliant, setter override a function

        override fun setFoo(value: Long) {
            foo = value
        }
    }

    abstract class AbstractGetter {
        // modifier 'abstract' is not applicable to Kotlin's property getters and setters

        private var foo: Long = 0
        private var bar: Long = 0
        abstract fun getFoo(): Long // Compliant, abstract getter
        fun setFoo(value: Long) { foo = value }

        fun getBar(): Long = bar // Compliant, abstract setter
        abstract fun setBar(value: Long)
    }

    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class Inject

    @Target(AnnotationTarget.FUNCTION)
    @Retention(AnnotationRetention.RUNTIME)
    annotation class ID

    class AnnotatedAccessors {
        private var id: Long = 0
        private var name: String = ""
        @Volatile
        private var address: String = ""

        @ID
        fun getId(): Long = id // Compliant, getter is annotated

        fun getName(): String = name // Compliant, setter is annotated
        @Inject
        fun setName(value: String) { name = value }

        fun getAddress(): String = address // Compliant, property is annotated
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

    private open class PrivateClassWithGenerics {
        private var value1: List<String> = listOf()
        //          ^^^^^^>
        fun getValue1(): List<String> = listOf() // Noncompliant
        //  ^^^^^^^^^
        fun setValue1(value: List<String>) {}
        //  ^^^^^^^^^<

        private var value2: List<String> = listOf()
        fun getValue2(): List<Int> = listOf() // Compliant, return "List<Int>" instead of "List<String>"
        fun setValue2(value: List<out String>) {} // Compliant, argument type is "List<out String>" instead of "List<String>"

        private var value3: Array<in String> = arrayOf()
        fun getValue3(): Array<String> = arrayOf() // Compliant, return "Array<String>" instead of "Array<in String>?"
        fun setValue3(value: Array<String>) {} // Compliant, argument type is "Array<String>" instead of "Array<in String>"

        private var value4: List<String>? = listOf()
        fun getValue4(): List<String> = listOf() // Compliant, return "List<String>" instead of "List<String>?"
        fun setValue4(value: List<String>) {} // Compliant, argument type is "List<String>" instead of "List<String>?"
    }

    private open class PrivateClassWithAnnotations {
        @Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
        @Retention(AnnotationRetention.RUNTIME)
        annotation class MyAnnotation1

        @Target(AnnotationTarget.TYPE, AnnotationTarget.TYPE_PARAMETER)
        @Retention(AnnotationRetention.RUNTIME)
        annotation class MyAnnotation2

       // property getter and setter support annotations
       var value1: String = ""
           get(): @MyAnnotation1 String = field.uppercase()
           set(value: @MyAnnotation2 String) { field = value.lowercase() }


        private var value2: String = ""
        //          ^^^^^^>
        fun getValue2(): @MyAnnotation1 String = "" // Noncompliant
        //  ^^^^^^^^^
        fun setValue2(value: @MyAnnotation2 String) { }
        //  ^^^^^^^^^<
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
    class IssueOnPublicClass {
        private val value: Int = 42
        fun getValue(): Int = value // Noncompliant
    }
}
