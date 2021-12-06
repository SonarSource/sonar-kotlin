package operators

import checks.MyUI
import checks.ResourceID
import kotlin.reflect.KProperty
import kotlin.properties.ReadOnlyProperty

class OperatorsContainer

operator fun OperatorsContainer.getValue(someClass: Any, property: KProperty<*>): String = "delegate"
operator fun OperatorsContainer.setValue(someClass: Any, property: KProperty<*>, value: String) {}


operator fun OperatorsContainer.get(i: Int) = this
operator fun OperatorsContainer.get(i: Int, j: Int) = this
operator fun OperatorsContainer.set(i: Int, o: OperatorsContainer) = o
operator fun OperatorsContainer.set(i: Int, j: Int, o: OperatorsContainer) = o
operator fun OperatorsContainer.unaryMinus() = this
operator fun OperatorsContainer.unaryPlus() = this
operator fun OperatorsContainer.not() = this
operator fun OperatorsContainer.inc() = this
operator fun OperatorsContainer.dec() = this
operator fun OperatorsContainer.plus(i: Int) = this
operator fun OperatorsContainer.minus(i: Int) = this
operator fun OperatorsContainer.times(i: Int) = this
operator fun OperatorsContainer.div(i: Int) = this
operator fun OperatorsContainer.rem(i: Int) = this
operator fun OperatorsContainer.rangeTo(i: Int) = this
operator fun OperatorsContainer.contains(i: Int) = true
operator fun OperatorsContainer.invoke(i: Int) = this
operator fun OperatorsContainer.plusAssign(i: Int) {}
operator fun OperatorsContainer.minusAssign(i: Int) {}
operator fun OperatorsContainer.timesAssign(i: Int) {}
operator fun OperatorsContainer.divAssign(i: Int) {}
operator fun OperatorsContainer.remAssign(i: Int) {}
operator fun <T>ResourceLoader<T>.provideDelegate(thisRef: MyUI, prop: KProperty<*>): ReadOnlyProperty<MyUI, T> {
    checkProperty(thisRef, prop.name)
    // create delegate
    return ResourceDelegate()
}

private fun checkProperty(thisRef: MyUI, name: String) {
    TODO()
}


class ResourceDelegate<T> : ReadOnlyProperty<MyUI, T> {
    override fun getValue(thisRef: MyUI, property: KProperty<*>): T {
        TODO()
    }
}

class ResourceLoader<T>(id: ResourceID<T>) {}
