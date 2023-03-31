package delegates

import kotlin.reflect.KProperty

inline operator fun <T> State<T>.getValue(thisObj: Any?, property: KProperty<*>): T = value

inline operator fun <T> State<T>.setValue(t: Any?, property: KProperty<*>, t1: T) {
    TODO("Not yet implemented")
}
interface State<out T> {
    val value: T
}
