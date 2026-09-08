package co.touchlab.kermit

open class Logger {
    fun v(message: String) = Unit
    fun d(message: String) = Unit
    fun d(message: () -> String) = Unit
    fun i(message: String) = Unit
    fun i(message: () -> String) = Unit
    fun w(message: String) = Unit
    fun e(message: String) = Unit
    fun a(message: String) = Unit
}
