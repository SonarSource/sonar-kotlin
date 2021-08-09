package checks

import java.io.File

class UnencryptedFilesInMobileApplicationsCheckSample {
    fun f(fileContent: String) {
        val targetFile = File("my_sensitive_data.txt")
        targetFile.writeText(fileContent)  // Noncompliant {{Make sure using unencrypted files is safe here.}}
//                 ^^^^^^^^^        
        targetFile.appendBytes(fileContent.toByteArray()) // Noncompliant
//                 ^^^^^^^^^^^        
    }
}
