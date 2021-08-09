package checks

import java.io.File

class UnencryptedFilesInMobileApplicationsCheckSampleNonAndroid {
    fun f(fileContent: String) {
        val targetFile = File("my_sensitive_data.txt")
        targetFile.writeText(fileContent) // Ok, not in an Android context
        targetFile.appendBytes(fileContent.toByteArray()) // Ok, not in an Android context
    }
}
