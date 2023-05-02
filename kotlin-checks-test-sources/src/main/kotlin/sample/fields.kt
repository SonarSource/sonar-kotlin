package sample

fun foo(
    string: String,
    collection: Collection<Int>,
    list: List<Int>,
    customizedList: CustomizedList,
    nonCollection: NonCollection,
    sample: Sample,
    listContainer: ListContainer
) {
    string.length       // Index: 11
    collection.size     // Index: 13
    collection.count()
    list.size           // Index: 17
    customizedList.size // Index: 19
    customizedList.zoodles // Index: 21
    nonCollection.size // Index: 23
    sample.name // Index: 25
    sample.pi // Index: 27
    listContainer.customizedList // Index: 29
    listContainer.customizedList.size // Index: 32
    listContainer.listContainer.customizedList.size // Index: 36
}

interface CustomizedList: List<Int> {
    val zoodles: Int
}

interface NonCollection {
    val size: Int
    fun count(): Int

    fun isEmpty(): Boolean

    fun isNotEmpty(): Boolean
}

class Sample {
    val name: String = ""
    val pi: Double = Math.PI
}

interface ListContainer {
    val customizedList: CustomizedList
    val listContainer: ListContainer
}
