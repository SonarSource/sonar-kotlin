package checks

class SimplifySizeExpressionCheckSample {

    fun wip(
        list: ArrayList<String>
    ) {
        if (list.size > 0) foo() // Noncompliant
    }

    fun sizeCheck(
        list: List<Int>,
        set: Set<Int>,
        map: Map<Int, String>,
        customizedList: CustomizedList,
        customizedListImpl: CustomizedListImpl,
        customizedMap: CustomizedMap,
        collection: Collection<Int>,
        nonCollection: NonCollection,
        string: String
    ) {
        if (list.size == 0) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
//          ^^^^^^^^^^^^^^
        if (list.size != 0) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
        if (list.size > 0) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
        if (list.size < 0) foo() // Compliant
        if (0 == list.size) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
        if (0 != list.size) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
        if (0 > list.size) foo() // Compliant
        if (0 < list.size) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
//          ^^^^^^^^^^^^^

        if (list != null && list.size != 0 + 42) foo() // Compliant

        if (list.size == 42) foo() // Compliant
        if (list.size > 42) foo() // Compliant
        if (42 < list.size) foo() // Compliant

        if (set.size == 0) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
        if (0 < set.size) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
        if (map.size == 0) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
        if (0 < map.size) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
        if (customizedList.size == 0) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
        if (0 < customizedList.size) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
        if (customizedListImpl.size == 0) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
        if (0 < customizedListImpl.size) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
        if (customizedMap.size == 0) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
        if (0 < customizedMap.size) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
        if (collection.size == 0) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
        if (0 < collection.size) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
        if (nonCollection.size == 0) foo() // Compliant
        if (0 < nonCollection.size) foo() // Compliant

        if (string.length == 0) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
        if (0 < string.length) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
    }

    //fun sizeAndNullCheck

    fun countCheck(
        list: List<Int>,
        map: Map<Int, String>,
        nonCollection: NonCollection,
        string: String
    ) {
        if (list.count() == 0) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
//          ^^^^^^^^^^^^^^^^^
        if (list.count() != 0) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
        if (list.count() > 0) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
        if (0 != list.count()) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
        if (0 < list.count()) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
//          ^^^^^^^^^^^^^^^^
        if (list.count() == 42) foo() // Compliant
        if (list.count(){ it < 42 } != 0) foo() // Compliant

        if (map.count() == 0) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
        if (0 < map.count()) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
        if (nonCollection.count() == 0) foo() // C
        if (0 < nonCollection.count()) foo() // C
        if (string.count() == 0) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
        if (0 < string.count()) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
    }

    fun emptyCheck(
        list: List<Int>,
        map: Map<Int, String>,
        nonCollection: NonCollection,
        string: String
    ) {
        if (list.isEmpty()) foo() // C
        if (list.isNotEmpty()) foo() // C
        if (!list.isEmpty()) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
//          ^^^^^^^^^^^^^^^
        if (!list.isNotEmpty()) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
        if (!!list.isEmpty()) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
//           ^^^^^^^^^^^^^^^
        if (!!list.isNotEmpty()) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}

        if (!map.isEmpty()) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
        if (!map.isNotEmpty()) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
        if (!nonCollection.isEmpty()) foo() // Compliant
        if (!nonCollection.isNotEmpty()) foo() // Compliant
        if (!string.isEmpty()) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
        if (!string.isNotEmpty()) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
    }

    fun nullCheck(
        list: List<Int>?,
        list2: List<Int>,
        map: Map<Int, String>?,
        nonCollection: NonCollection?,
        string: String?
    ) {
        if (list != null && list.size != 0) foo() // Noncompliant {{Replace null check and collection size check with "!isNullOrEmpty()"}}
//          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        if (list != null && list.size > 0) foo() // Noncompliant {{Replace null check and collection size check with "!isNullOrEmpty()"}}
//          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        if (list != null && 0 < list.size) foo() // Noncompliant {{Replace null check and collection size check with "!isNullOrEmpty()"}}
//          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        if (list != null && list.size == 0) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
//                          ^^^^^^^^^^^^^^

        if (list == null && list2.size != 0) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
//                          ^^^^^^^^^^^^^^^
        if (list == null && list2.size > 0) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
//                          ^^^^^^^^^^^^^^
        if (list == null && 0 < list2.size) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
//                          ^^^^^^^^^^^^^^
        if (list == null && list2.size == 0) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
//                          ^^^^^^^^^^^^^^^

        if (list2.size != 0 && list2 != null) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
//          ^^^^^^^^^^^^^^^
        if (list2.size == 0 && list2 != null) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
        if (list2.size != 0 && list2 == null) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
        if (list2.size == 0 && list2 == null) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}

        if (list == null || list.size != 0) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
//                          ^^^^^^^^^^^^^^
        if (list == null || list.size > 0) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
//                          ^^^^^^^^^^^^^
        if (list == null || 0 == list.size) foo() // Noncompliant {{Replace null check and collection size check with "isNullOrEmpty()"}}
//          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        if (list == null || list.size == 0) foo() // Noncompliant {{Replace null check and collection size check with "isNullOrEmpty()"}}
        if (0 == list2.size || list == null) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
//          ^^^^^^^^^^^^^^^
        if (list2.size != 0 || list == null) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}

        if (list != null && list.isNotEmpty()) foo() // Noncompliant {{Replace null check and collection size check with "!isNullOrEmpty()"}}
        if (list != null && list.isEmpty()) foo() // Compliant
        if (list == null && list2.isNotEmpty()) foo() // Compliant
        if (list == null && list2.isEmpty()) foo() // Compliant

        if (list == null || list.isEmpty()) foo() // Noncompliant {{Replace null check and collection size check with "isNullOrEmpty()"}}
        if (list == null || list.isNotEmpty()) foo() // Compliant
        if (list != null || list2.isEmpty()) foo() // Compliant
        if (list != null || list2.isNotEmpty()) foo() // Compliant

        if (list2.isNotEmpty() && list2 != null) foo() // Compliant
        if (list2.isEmpty() && list2 != null) foo() // Compliant
        if (list2.isNotEmpty() && list2 == null) foo() // Compliant
        if (list2.isEmpty() && list2 == null) foo() // Compliant

        if (list != null && !list.isEmpty()) foo() // Noncompliant {{Replace null check and collection size check with "!isNullOrEmpty()"}}
//          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        if (list != null && !list.isNotEmpty()) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
//                          ^^^^^^^^^^^^^^^^^^
        if (list == null && !list2.isEmpty()) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
//                          ^^^^^^^^^^^^^^^^
        if (list == null && !list2.isNotEmpty()) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}

        if (list == null || !list.isNotEmpty()) foo() // Noncompliant {{Replace null check and collection size check with "isNullOrEmpty()"}}
//          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        if (list == null || !list.isEmpty()) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
//                          ^^^^^^^^^^^^^^^
        if (list != null || !list2.isNotEmpty()) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
//                          ^^^^^^^^^^^^^^^^^^^
        if (list != null || !list2.isEmpty()) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}

        if (map != null && map.size != 0) foo() // Noncompliant {{Replace null check and collection size check with "!isNullOrEmpty()"}}
        if (map == null || map.size == 0) foo() // Noncompliant {{Replace null check and collection size check with "isNullOrEmpty()"}}
        if (map != null && map.isNotEmpty()) foo() // Noncompliant {{Replace null check and collection size check with "!isNullOrEmpty()"}}
        if (map == null || map.isEmpty()) foo() // Noncompliant {{Replace null check and collection size check with "isNullOrEmpty()"}}
        if (map != null && !map.isEmpty()) foo() // Noncompliant {{Replace null check and collection size check with "!isNullOrEmpty()"}}
        if (map != null && !map.isNotEmpty()) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
//                         ^^^^^^^^^^^^^^^^^

        if (nonCollection != null && nonCollection.size != 0) foo() // Compliant
        if (nonCollection == null || nonCollection.size == 0) foo() // Compliant
        if (nonCollection != null && nonCollection.isNotEmpty()) foo() // Compliant
        if (nonCollection == null || nonCollection.isEmpty()) foo() // Compliant
        if (nonCollection != null && !nonCollection.isEmpty()) foo() // Compliant
        if (nonCollection != null && !nonCollection.isNotEmpty()) foo() // Compliant

        if (string != null && string.length != 0) foo() // Noncompliant {{Replace null check and collection size check with "!isNullOrEmpty()"}}
        if (string == null || string.length == 0) foo() // Noncompliant {{Replace null check and collection size check with "isNullOrEmpty()"}}
        if (string != null && string.isNotEmpty()) foo() // Noncompliant {{Replace null check and collection size check with "!isNullOrEmpty()"}}
        if (string == null || string.isEmpty()) foo() // Noncompliant {{Replace null check and collection size check with "isNullOrEmpty()"}}
        if (string != null && !string.isEmpty()) foo() // Noncompliant {{Replace null check and collection size check with "!isNullOrEmpty()"}}
        if (string != null && !string.isNotEmpty()) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
    }

    fun parenthesized(list: List<Int>) {
        if ((list != null) && (0 != list.size)) foo() // Noncompliant {{Replace null check and collection size check with "!isNullOrEmpty()"}}
//          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        if ((list != null) && (list.size != 0)) foo() // Noncompliant {{Replace null check and collection size check with "!isNullOrEmpty()"}}
//          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        if (((list != null)) && ((list.size > 0))) foo() // Noncompliant {{Replace null check and collection size check with "!isNullOrEmpty()"}}
//          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    }

    fun complexAccessors(list: CustomizedList?, container: ListContainer) {
        if (container.containers.first().containers[42].list.size == 0) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
//          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        if (container.containers.first().containers[42].list.count() != 0) foo() // Noncompliant {{Replace collection size check with "isNotEmpty()"}}
        if (!container.containers.first().containers[42].list.isNotEmpty()) foo() // Noncompliant {{Replace collection size check with "isEmpty()"}}
    }
}

private fun foo(): Unit = TODO()

interface CustomizedList: List<Int>

abstract class CustomizedListImpl: CustomizedList

interface CustomizedMap: List<Int>

interface NonCollection {
    val size: Int
    fun count(): Int

    fun isEmpty(): Boolean

    fun isNotEmpty(): Boolean
}

interface ListContainer {
    val list: CustomizedList
    val containers: Array<ListContainer>

    fun getContainer(): ListContainer
}
