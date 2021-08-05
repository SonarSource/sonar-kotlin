package android.os

import java.io.File

class Environment {
    fun getExternalStorageDirectory(): File {
        throw NotImplementedError()
    }

    fun getExternalStoragePublicDirectory(type: String): File {
        throw NotImplementedError()
    }

    fun getDownloadCacheDirectory(): File {
        throw NotImplementedError()
    }
}
