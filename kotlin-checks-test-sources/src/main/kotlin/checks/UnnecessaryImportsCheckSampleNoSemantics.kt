package checks

// same package/file
import checks.ClassInSameFileB // Noncompliant {{Remove this redundant import.}}
// same package
import checks.DelicateCoroutinesApi // Noncompliant {{Remove this redundant import.}}
// unused
import com.google.common.collect.ImmutableList // Noncompliant {{Remove this unused import.}}
// unused
import com.google.common.collect.ImmutableList.copyOf // Noncompliant {{Remove this unused import.}}
import com.google.common.collect.ImmutableList.of
import com.google.common.collect.ImmutableSet.of as immutable_set_of
// unused
import com.google.common.io.Files // Noncompliant {{Remove this unused import.}}
import com.google.common.io.Files.isFile
import okhttp3.TlsVersion
import org.apache.commons.lang.StringUtils
import otherpackage.get
// not used, defaults to String.plus
import otherpackage.plus // FN due to missing binding context
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
import operators.OperatorsContainer
import otherpackage.OtherClass
import otherpackage.OtherClass2.plus
import otherpackage.OtherClass2.get // FN due to missing binding context
import otherpackage.OtherClass2.set // FN due to missing binding context
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
import operators.getValue
import operators.setValue // FN due to missing binding context
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
import operators.provideDelegate // FN due to missing binding context

class SomeClassWithDelegateNoSemantics(var delegate: OperatorsContainer) {
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


class UnnecessaryImportsCheckSampleNoSemantics {
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
        
        
    }

    /**
     * This is a KDoc comment referencing [java.util.Currency] by FQN but java.util.[EventObject] by simple name
     * @see BitSet
     * @see java.util.Base64
     */
    fun File.extensionFun() {}

    lateinit var b : ClassInSameFileB

    @DelicateCoroutinesApi
    fun bar() {

    }
}

class ChildClass1B: java.util.Date()
class ChildClass2B: Timer()
class ClassInSameFileB
