package checks

import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path

class UnencryptedFilesInMobileApplicationsCheckSample {
    fun f(fileContent: String) {
        val targetFile = File("my_sensitive_data.txt")
        targetFile.writeText(fileContent)  // Noncompliant {{Make sure using unencrypted files is safe here.}}
//                 ^^^^^^^^^        
        targetFile.appendBytes(fileContent.toByteArray()) // Noncompliant
//                 ^^^^^^^^^^^        

        val fileWriter = FileWriter("my_sensitive_data.txt") // Noncompliant

        val fileOutputStream = FileOutputStream("my_sensitive_data.txt") // Noncompliant
        
        Files.write(Path.of("my_sensitive_data.txt"), fileContent.toByteArray()) // Noncompliant
    }
}
