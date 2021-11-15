package checks

import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Path

class UnencryptedFilesInMobileApplicationsCheckSampleNonAndroid {
    fun f(fileContent: String) {
        val targetFile = File("my_sensitive_data.txt")
        targetFile.writeText(fileContent) // Ok, not in an Android context
        targetFile.appendBytes(fileContent.toByteArray()) // Ok, not in an Android context

        val fileWriter = FileWriter("my_sensitive_data.txt")

        val fileOutputStream = FileOutputStream("my_sensitive_data.txt")

        Files.write(Path.of("my_sensitive_data.txt"), fileContent.toByteArray())
    }
}
