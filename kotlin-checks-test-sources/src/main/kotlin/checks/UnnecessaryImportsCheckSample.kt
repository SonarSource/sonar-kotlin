package checks

// same package/file
import checks.ClassInSameFileA // Noncompliant {{Remove this redundant import.}}
//     ^^^^^^^^^^^^^^^^^^^^^^^
// same package
import checks.DelicateCoroutinesApi // Noncompliant {{Remove this redundant import.}}
// unused
import com.google.common.collect.ImmutableList // Noncompliant {{Remove this unused import.}}
// unused
import com.google.common.collect.ImmutableList.copyOf // Noncompliant {{Remove this unused import.}}
//     ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
import com.google.common.collect.ImmutableList.of
import com.google.common.collect.ImmutableSet.of as immutable_set_of
// unused
import com.google.common.io.Files // Noncompliant {{Remove this unused import.}}
import com.google.common.io.Files.isFile
import operators.OperatorsContainer
import imports.ClassWithCompanionObject
import imports.ClassWithNamedCompanionObject
import okhttp3.TlsVersion
import org.apache.commons.lang.StringUtils
import otherpackage.get
// not used, defaults to String.plus
import otherpackage.plus // Noncompliant {{Remove this unused import.}}
import otherpackage.OtherClass.minus
import otherpackage.OtherClass.plus // Non|compliant FN (it is used below in OtherClass + OtherClass but doesn't need to be imported)
import otherpackage.OtherClass.get
import otherpackage.OtherClass.set
import java.io.File
import java.lang.StringBuilder
import java.nio.file.Path
// fully qualified name is used in KDoc
import java.util.Currency // Noncompliant {{Remove this unused import.}}
import java.util.EventObject // Compliant (KDoc usage)
// fully qualified name is used in KDoc
import java.util.Base64 // Noncompliant {{Remove this unused import.}}
import java.util.BitSet // Compliant (KDoc usage)
// fully qualified name is used below
import java.util.Date // Noncompliant {{Remove this unused import.}}
import java.util.Timer
// kotlin.* is automatically imported
import kotlin.Any // Noncompliant {{Remove this redundant import.}}
import okhttp3.TlsVersion.TLS_1_1
// unused
import okhttp3.TlsVersion.TLS_1_2 // Noncompliant {{Remove this unused import.}}
import otherpackage.OtherClass
import otherpackage.OtherClass2.plus
import otherpackage.OtherClass2.get // Noncompliant {{Remove this unused import.}}
import otherpackage.OtherClass2.set // Noncompliant {{Remove this unused import.}}
import otherpackage.someInfixFun
import otherpackage.stringExtFun1
import otherpackage.stringExtFun2
import java.io.InputStream
import java.lang.reflect.Method
import kotlin.reflect.jvm.kotlinFunction
import okhttp3.TlsVersion.SSL_3_0 as TLS3 // Noncompliant {{Remove this unused import.}}
import okhttp3.TlsVersion.TLS_1_3 as TLS13
import java.beans.* // Non|compliant FN (we currently ignore all wildcard imports)
import kotlin.test.* // Non|compliant FN (we currently ignore all wildcard imports)
// Except for this one
import kotlin.* // Noncompliant {{Remove this redundant import.}}

// Operators
import operators.getValue // Compliant is a delegation operator
import operators.setValue // Noncompliant
import operators.contains
import operators.dec
import operators.div
import operators.inc
import operators.minus
import operators.not
import operators.plus
import operators.rangeTo
import operators.rem
import operators.times
import operators.unaryMinus
import operators.unaryPlus
import operators.get
import operators.invoke
import operators.set
import operators.plusAssign // Noncompliant
import operators.minusAssign // Noncompliant
import operators.timesAssign // Noncompliant
import operators.divAssign // Noncompliant
import operators.remAssign // Noncompliant


import operators.ResourceLoader
import operators.provideDelegate // Compliant


class MyUI {
    fun <T> bindResource(id: ResourceID<T>): ResourceLoader<T> {
        TODO()
    }

    val image by bindResource(ResourceID.image_id)
    val text by bindResource(ResourceID.text_id)
}

class ResourceID<T> {
    companion object {
        val image_id: ResourceID<BitSet> = TODO()
        val text_id: ResourceID<String> = TODO()

    }
}

class SomeClassWithDelegate(var delegate: OperatorsContainer) {
    val someProperty: String by delegate

    fun test() {
        +delegate
        -delegate
        !delegate
        delegate++
        delegate--
        --delegate
        ++delegate
        delegate + 1
        delegate - 1
        delegate * 1
        delegate / 1
        delegate % 1
        delegate .. 1
        1 in delegate
        2 !in delegate
        delegate[1,2]
        delegate[1,2] = delegate
        delegate(0)
    }
}

class UnnecessaryImportsCheckSample {
    fun foo() {
        StringBuilder()
        StringUtils.EMPTY
        var x: Path? = null
        TlsVersion.TLS_1_0
        isFile()
        Path.of("")
        val a: Any = ""
        a[1]
        "foo" + "bar"
        1[5]
        1[5] = 5
        OtherClass + OtherClass
        InputStream.nullInputStream() - InputStream.nullInputStream()
        InputStream.nullInputStream() + InputStream.nullInputStream()
        of("String")
        TLS_1_1
        val normalArray = arrayOf("foo")
        normalArray[0]
        "foo".stringExtFun1()
        "foo".run {
            stringExtFun2()
        }
        TLS13
        `immutable_set_of`("")
        "" someInfixFun ""
        val method: Method? = null
        val kf = method!!.kotlinFunction

        val c = ClassWithCompanionObject.MY_CONSTANT // Compliant

        ClassWithNamedCompanionObject.companionObjectFun()
    }

    /**
     * This is a KDoc comment referencing [java.util.Currency] by FQN but java.util.[EventObject] by simple name
     * @see BitSet
     * @see java.util.Base64
     */
    fun File.extensionFun() {}

    lateinit var b : ClassInSameFileA

    @DelicateCoroutinesApi
    fun bar() {

    }
}

class ChildClass1A: java.util.Date()
class ChildClass2A: Timer()
class ClassInSameFileA
