package checks

import checks.ClassInSameFileA
import checks.DelicateCoroutinesApi
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableList.copyOf
import com.google.common.collect.ImmutableList.of
import com.google.common.collect.ImmutableSet.of as immutable_set_of
import com.google.common.io.Files
import com.google.common.io.Files.isFile
import imports.ClassWithCompanionObject
import imports.ClassWithNamedCompanionObject
import okhttp3.TlsVersion
import org.apache.commons.lang.StringUtils
import otherpackage.get
import otherpackage.plus
import otherpackage.OtherClass.minus
import otherpackage.OtherClass.plus
import otherpackage.OtherClass.get
import otherpackage.OtherClass.set
import java.io.File
import java.lang.StringBuilder
import java.nio.file.Path
import java.util.Currency // Noncompliant {{Remove this unused import.}}
import java.util.EventObject // Compliant (KDoc usage)
import java.util.Base64 // Noncompliant {{Remove this unused import.}}
import java.util.BitSet // Compliant (KDoc usage)
import java.util.Date // Noncompliant {{Remove this unused import.}}
import java.util.Timer // Noncompliant {{Remove this unused import.}}
import okhttp3.TlsVersion.TLS_1_1
import okhttp3.TlsVersion.TLS_1_2
import otherpackage.OtherClass
import otherpackage.OtherClass2.plus
import otherpackage.OtherClass2.get
import otherpackage.OtherClass2.set
import otherpackage.someInfixFun
import otherpackage.stringExtFun1
import otherpackage.stringExtFun2
import java.io.InputStream
import java.lang.reflect.Method
import kotlin.reflect.jvm.kotlinFunction
import okhttp3.TlsVersion.SSL_3_0 as TLS3
import okhttp3.TlsVersion.TLS_1_3 as TLS13
import java.beans.*
import kotlin.test.*
import kotlin.* // Noncompliant {{Remove this redundant import.}}

class UnnecessaryImportsCheckSamplePartialSemantics {
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
