package android.util

object Log {
    fun v(tag: String, message: String) = Unit
    fun d(tag: String, message: String) = Unit
    fun i(tag: String, message: String) = Unit
    fun w(tag: String, message: String) = Unit
    fun e(tag: String, message: String) = Unit
    fun wtf(tag: String, message: String) = Unit
    fun println(priority: Int, tag: String, message: String) = Unit
}
