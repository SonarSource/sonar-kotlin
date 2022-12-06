package checks

import java.util.stream.DoubleStream
import java.util.stream.IntStream
import java.util.stream.LongStream
import java.util.stream.Stream

class StreamNotConsumedCheckSample {

    fun testSequence(source: Sequence<String>, dest: MutableList<String>) {
        source.associate { it.length to it } // Compliant
        source.associateBy { it.length } // Compliant
        source.associateByTo(mutableMapOf()) { it.length } // Compliant
        source.associateTo(mutableMapOf()) { it.length to it } // Compliant
        source.associateWith { it.length } // Compliant
        source.associateWithTo(mutableMapOf()) { it.length } // Compliant
        source.chunked(12) // Noncompliant {{Refactor the code so this sequence pipeline is used.}}
        //     ^^^^^^^
        source.chunked(12, { it }) // Noncompliant
        source.distinct() // Noncompliant
        source.distinctBy { it.hashCode() } // Noncompliant
        source.drop(2) // Noncompliant
        source.dropWhile { it.length < 2 }  // Noncompliant
        source.filter { it.isNotBlank() } // Noncompliant
        source.filter(String::isNotBlank) // Noncompliant
        source.filter(String::isNotBlank).forEach(::println) // Compliant
        source.filterIndexed { i, s -> i == 0 || s.isNotBlank() } // Noncompliant
        source.filterIndexedTo(dest) { i, s -> i == 0 || s.isNotBlank() } // Compliant
        source.filterIsInstance(String::class.java) // Noncompliant
        source.filterIsInstance<String>() // Noncompliant
        source.filterIsInstanceTo(dest, String::class.java) // Compliant
        source.filterNot { it.isNotBlank() } // Noncompliant
        source.filterNotNull() // Noncompliant
        source.filterNotNull().count() // Compliant
        source.filterNotNull().filter(String::isNotBlank).forEach(::println) // Compliant
        source.filterNotNullTo(dest) // Compliant
        source.filterNotTo(dest, String::isNotBlank) // Compliant
        source.filterTo(dest, String::isNotBlank) // Compliant
        source.flatMap { sequenceOf(it) } // Noncompliant
        source.flatMapTo(dest) { sequenceOf(it) } // Compliant
        source.flatMapIndexed { i,v -> sequenceOf(i.toString(), v) } // Noncompliant
        source.flatMapIndexedTo(dest) { i,v -> sequenceOf(i.toString(), v) } // Compliant
        source.groupBy { it.length } // Compliant
        source.groupByTo(mutableMapOf()) { it.length } // Compliant
        source.groupingBy { it.length } // Compliant
        source.map { it.uppercase() } // Noncompliant {{Refactor the code so this sequence pipeline is used.}}
        source.mapIndexed { i, s -> i.toString() + s } // Noncompliant
        source.mapIndexedNotNull { i, s -> i.toString() + s } // Noncompliant
        source.mapIndexedTo(dest) { i, s -> i.toString() + s } // Compliant
        source.mapNotNull { it.uppercase() } // Noncompliant
        source.mapNotNullTo(dest) { it.uppercase() } // Compliant
        source.mapTo(dest) { it.uppercase() } // Compliant
        source.plus("") // Noncompliant
        source.plus(arrayOf("")) // Noncompliant
        source.plus(listOf("")) // Noncompliant
        source.plus(sequenceOf("")) // Noncompliant
        source.plusElement("") // Noncompliant
        source.shuffled() // Compliant
        source.shuffled(kotlin.random.Random.Default) // Compliant
        source.sorted() // Noncompliant
        source.sortedBy { it.length } // Noncompliant
        source.sortedByDescending { it[0] } // Noncompliant
        source.sortedDescending() // Noncompliant
        source.sortedWith(Comparator.comparing { it }) // Noncompliant
        source.take(5) // Noncompliant
        source.takeWhile { it.isNotBlank() } // Noncompliant
        source.zip(sequenceOf(1)) // Noncompliant
        source.zip(sequenceOf(1), { e, k -> e + k }) // Noncompliant
        source.zipWithNext() // Noncompliant
        source.zipWithNext({ e, k -> e + k }) // Noncompliant
    }

    fun testList(source: List<String>, dest: MutableList<String>) {
        source.stream() // Noncompliant {{Refactor the code so this stream pipeline is used.}}
        //     ^^^^^^
        source.parallelStream() // Noncompliant
        source.chunked(12) // Compliant
        source.chunked(12, { it }) // Compliant
        source.distinct() // Compliant
        source.distinctBy { it.hashCode() } // Compliant
        source.drop(2) // Compliant
        source.dropWhile { it.length < 2 }  // Compliant
        source.filter { it.isNotBlank() } // Compliant
        source.filter(String::isNotBlank) // Compliant
        source.filterIndexed { i, s -> i == 0 || s.isNotBlank() } // Compliant
        source.filterIsInstance(String::class.java) // Compliant
        source.filterIsInstance<String>() // Compliant
        source.filterNot { it.isNotBlank() } // Compliant
        source.filterNotNull() // Compliant
        source.map { it.uppercase() } // Compliant
        source.mapIndexed { i, s -> i.toString() + s } // Compliant
        source.mapIndexedNotNull { i, s -> i.toString() + s } // Compliant
        source.mapNotNull { it.uppercase() } // Compliant
        source.plus("") // Compliant
        source.plus(arrayOf("")) // Compliant
        source.plus(listOf("")) // Compliant
        source.plus(sequenceOf("")) // Compliant
        source.plusElement("") // Compliant
        source.take(5) // Compliant
        source.takeWhile { it.isNotBlank() } // Compliant
        source.zip(listOf(1)) // Compliant
        source.zip(listOf(1), { e, k -> e + k }) // Compliant
        source.zipWithNext() // Compliant
        source.zipWithNext({ e, k -> e + k }) // Compliant
    }

    fun testMap(source: Map<Int, String>) {
        source.filter { it.key > 10 } // Compliant
        source.filterKeys { it > 10 } // Compliant
        source.filterNot { it.key > 10 } // Compliant
        source.filterValues { it.isNotBlank() } // Compliant
        source.map { it } // Compliant
        source.mapKeys { it.key + 1 } // Compliant
        source.mapKeysTo(mutableMapOf()) { it.key + 1 } // Compliant
        source.mapNotNull { it } // Compliant
        source.mapNotNullTo(mutableListOf()) { it } // Compliant
        source.mapTo(mutableListOf()) { it } // Compliant
        source.mapValues { it.value.uppercase() } // Compliant
        source.mapValuesTo(mutableMapOf()) { it.value.uppercase() } // Compliant
        source.plus(1 to "") // Compliant
        source.plus(mapOf(1 to "")) // Compliant
        source.plus(listOf(1 to "")) // Compliant
        source.plus(arrayOf(1 to "")) // Compliant
    }

    fun testStream(
        stream: Stream<String>,
        intStream: IntStream,
        longStream: LongStream,
        doubleStream: DoubleStream,
    ) {
        stream.filter(String::isNotBlank) // Noncompliant {{Refactor the code so this stream pipeline is used.}}
        //     ^^^^^^
        stream.skip(2) // Noncompliant
        stream.limit(100) // Noncompliant
        stream.distinct() // Noncompliant
        stream.dropWhile { it.isBlank() } // Noncompliant
        stream.flatMap { Stream.of(it) } // Noncompliant
        stream.flatMap { Stream.of(it) }.forEach(::println) // Compliant
        stream.flatMapToInt { it.codePoints() } // Noncompliant
        stream.flatMapToLong { it.codePoints().mapToLong(Int::toLong) } // Noncompliant
        stream.flatMapToDouble { it.codePoints().mapToDouble(Int::toDouble) } // Noncompliant
        stream.map { it.uppercase() } // Noncompliant
        stream.mapToDouble { it.length.toDouble() } // Noncompliant
        stream.mapToInt { it.length } // Noncompliant
        stream.mapToLong { it.length.toLong() } // Noncompliant
        stream.parallel() // Noncompliant
        stream.peek { println(it) } // Noncompliant
        stream.sequential() // Noncompliant
        stream.sorted() // Noncompliant
        stream.sorted { o1, o2 -> o1.compareTo(o2) } // Noncompliant
        stream.takeWhile { it.isNotBlank() } // Noncompliant
        stream.unordered() // Noncompliant

        intStream.asDoubleStream() // Noncompliant
        intStream.asLongStream() // Noncompliant
        intStream.boxed() // Noncompliant
        intStream.filter { it > 100 } // Noncompliant
        intStream.distinct() // Noncompliant
        intStream.dropWhile { it > 100 }  // Noncompliant
        intStream.flatMap { IntStream.of(it) } // Noncompliant
        intStream.flatMap { IntStream.of(it) }.forEach(::println) // Compliant
        intStream.limit(100) // Noncompliant
        intStream.map { it + 1 } // Noncompliant
        intStream.mapToDouble { it.toDouble() } // Noncompliant
        intStream.mapToLong { it.toLong() } // Noncompliant
        intStream.mapToObj { it.toString() } // Noncompliant
        intStream.parallel() // Noncompliant
        intStream.peek { println(it) } // Noncompliant
        intStream.sequential() // Noncompliant
        intStream.skip(5) // Noncompliant
        intStream.sorted() // Noncompliant
        intStream.takeWhile { it > 1000 } // Noncompliant
        intStream.unordered() // Noncompliant

        longStream.asDoubleStream() // Noncompliant
        longStream.boxed() // Noncompliant
        longStream.filter { it > 100 } // Noncompliant
        longStream.distinct() // Noncompliant
        longStream.dropWhile { it > 100 }  // Noncompliant
        longStream.flatMap { LongStream.of(it) } // Noncompliant
        longStream.flatMap { LongStream.of(it) }.forEach(::println) // Compliant
        longStream.limit(100) // Noncompliant
        longStream.map { it + 1 } // Noncompliant
        longStream.mapToDouble { it.toDouble() } // Noncompliant
        longStream.mapToInt { it.toInt() } // Noncompliant
        longStream.mapToObj { it.toString() } // Noncompliant
        longStream.parallel() // Noncompliant
        longStream.peek { println(it) } // Noncompliant
        longStream.sequential() // Noncompliant
        longStream.skip(5) // Noncompliant
        longStream.sorted() // Noncompliant
        longStream.takeWhile { it > 1000 } // Noncompliant
        longStream.unordered() // Noncompliant

        doubleStream.boxed() // Noncompliant
        doubleStream.filter { it > 100 } // Noncompliant
        doubleStream.distinct() // Noncompliant
        doubleStream.dropWhile { it > 100 }  // Noncompliant
        doubleStream.flatMap { DoubleStream.of(it) } // Noncompliant
        doubleStream.flatMap { DoubleStream.of(it) }.forEach(::println) // Compliant
        doubleStream.limit(100) // Noncompliant
        doubleStream.map { it + 1 } // Noncompliant
        doubleStream.mapToInt { it.toInt() } // Noncompliant
        doubleStream.mapToLong { it.toLong() } // Noncompliant
        doubleStream.mapToObj { it.toString() } // Noncompliant
        doubleStream.parallel() // Noncompliant
        doubleStream.peek { println(it) } // Noncompliant
        doubleStream.sequential() // Noncompliant
        doubleStream.skip(5) // Noncompliant
        doubleStream.sorted() // Noncompliant
        doubleStream.takeWhile { it > 1000 } // Noncompliant
        doubleStream.unordered() // Noncompliant {{Refactor the code so this stream pipeline is used.}}
        //           ^^^^^^^^^
    }

}
