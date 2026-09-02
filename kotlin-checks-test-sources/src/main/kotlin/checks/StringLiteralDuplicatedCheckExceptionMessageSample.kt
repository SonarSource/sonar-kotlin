package checks

class StringLiteralDuplicatedCheckExceptionMessageSample {

    fun directThrownException(): Nothing =
        throw IllegalArgumentException("direct thrown exception message!")

    fun anotherDirectThrownException(): Nothing =
        throw IllegalStateException("direct thrown exception message!")

    fun thirdDirectThrownException(): Nothing =
        throw UnsupportedOperationException("direct thrown exception message!")

    fun concatenatedDirectThrownException(suffix: String): Nothing =
        throw IllegalArgumentException("concatenated thrown message: " + suffix)

    fun anotherConcatenatedDirectThrownException(suffix: String): Nothing =
        throw IllegalStateException("concatenated thrown message: " + suffix)

    fun thirdConcatenatedDirectThrownException(suffix: String): Nothing =
        throw UnsupportedOperationException("concatenated thrown message: " + suffix)

    fun thrownFactoryCall(): Nothing =
        // Noncompliant@+1
        throw exceptionWithMessage("factory call remains in scope!")

    fun anotherThrownFactoryCall(): Nothing =
        throw exceptionWithMessage("factory call remains in scope!")

    fun thirdThrownFactoryCall(): Nothing =
        throw exceptionWithMessage("factory call remains in scope!")

    fun standaloneExceptionConstructionRemainsInScope() {
        // Noncompliant@+1
        IllegalArgumentException("constructed but not thrown!")
        IllegalArgumentException("constructed but not thrown!")
        IllegalArgumentException("constructed but not thrown!")
    }

    fun kotlinError1(): Nothing = error("Kotlin error message!")
    fun kotlinError2(): Nothing = error("Kotlin error message!")
    fun kotlinError3(): Nothing = error("Kotlin error message!")

    fun kotlinRequire(condition: Boolean) {
        require(condition) { "Kotlin require message!" }
        require(condition) { "Kotlin require message!" }
        require(condition) { "Kotlin require message!" }
    }

    fun kotlinCheck(condition: Boolean) {
        check(condition) { "Kotlin check message!" }
        check(condition) { "Kotlin check message!" }
        check(condition) { "Kotlin check message!" }
    }

    fun customSameNamedFunctionsRemainInScope(custom: CustomExceptionFunctions, condition: Boolean) {
        // Noncompliant@+1
        custom.error("custom exception function message!")
        custom.require(condition) { "custom exception function message!" }
        custom.check(condition) { "custom exception function message!" }
    }
}

fun exceptionWithMessage(message: String): IllegalArgumentException = IllegalArgumentException(message)

class CustomExceptionFunctions {
    fun error(message: String) = Unit
    fun require(condition: Boolean, lazyMessage: () -> Any) = Unit
    fun check(condition: Boolean, lazyMessage: () -> Any) = Unit
}
