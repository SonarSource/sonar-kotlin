package checks

import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

class ObjectOutputStreamCheckSample {
    
    fun noncompliant_1(fileName: String?) {
        val fos = FileOutputStream(fileName, true) // fos opened in append mode
        val out = ObjectOutputStream(fos) // Noncompliant {{Do not use a FileOutputStream in append mode.}}
    }

    
    fun noncompliant_2(fileName: String?, appendMode: Boolean) {
        if (!appendMode) return
        val fos = FileOutputStream(fileName, appendMode) // fos opened in append mode
        val out = ObjectOutputStream(fos) // FN
    }

    
    fun noncompliant_3(file: File?) {
        val fos = FileOutputStream(file, true) // fos opened in append mode
        val out = ObjectOutputStream(fos) // Noncompliant
    }

    
    fun noncompliant_10() {
        val fos = Files.newOutputStream(
            Paths.get("a"),
            StandardOpenOption.APPEND
        )
        val out = ObjectOutputStream(fos) // Noncompliant [[flows=f1]]
    }

    
    fun noncompliant_11() {
        val fos = Files.newOutputStream(Paths.get("a"), StandardOpenOption.DELETE_ON_CLOSE, StandardOpenOption.APPEND)
        val out = ObjectOutputStream(fos) // Noncompliant
    }

    
    fun noncompliant_12() {
        val openOption: OpenOption = StandardOpenOption.APPEND
        val fos = Files.newOutputStream(Paths.get("a"), StandardOpenOption.DELETE_ON_CLOSE, openOption)
        val out = ObjectOutputStream(fos) // Noncompliant
    }

    
    fun noncompliant_13() {
        val fos = Files.newOutputStream(Paths.get("a"), StandardOpenOption.APPEND)
        val out = ObjectOutputStream(fos) // Noncompliant
    }

    
    fun compliant_1(fileName: String?) {
        val fos = FileOutputStream(fileName, false)
        val out = ObjectOutputStream(fos)
    }

    
    fun compliant_2(fileName: String?) {
        val fos = FileOutputStream(fileName)
        val out = ObjectOutputStream(fos)
    }

    
    fun compliant_10() {
        val fos = Files.newOutputStream(Paths.get("a"), StandardOpenOption.TRUNCATE_EXISTING)
        val out = ObjectOutputStream(fos)
    }

    
    fun coverage() {
        val out = ObjectOutputStream(null)
    }
}
