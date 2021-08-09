package checks

import io.realm.RealmConfiguration
import net.sqlcipher.database.SQLiteDatabase

// Noncompliant@+4 {{The "password" parameter should not be hardcoded.}}
fun checkSecondary() {
    val x  = getKey333()
//  ^^^^^^^^^^^^^^^^^^^^>    
    val database6 = SQLiteDatabase.openOrCreateDatabase("test.db", x, null)
//                                                                 ^
}

private fun getKey333(): ByteArray {
    return byteArrayOf(
//         ^^^^^^^^^^^<
        0,
        1,
        2,
    )
}

class MobileDatabaseEncryptionKeysCheckSample(val k: Key) {

    fun realm(s: String) {
        val stringKey = "d2345678e012345678901c3456789012a456789012n45678901234o678901227" // Secondary location here
        val stringKey2 = (((("d2345678e012345678901c3456789012a456789012n45678901234o678901227")))) // Secondary location here

        val config = RealmConfiguration.Builder()
            .name("noncompliant-S6301-realm.db")
            .encryptionKey(stringKey.toByteArray()) // Noncompliant {{The "encryptionKey" parameter should not be hardcoded.}}
            .build()
        
        val config0 = RealmConfiguration.Builder()
            .name("noncompliant-S6301-realm.db")
            .encryptionKey(stringKey2.toByteArray()) // Noncompliant {{The "encryptionKey" parameter should not be hardcoded.}}
            .build()

        val config1 = RealmConfiguration.Builder()
            .name("noncompliant-S6301-realm.db")
            .encryptionKey(getKey()) // Noncompliant
            .build()

        val config2 = RealmConfiguration.Builder()
            .name("noncompliant-S6301-realm.db")
            .encryptionKey(getKey2()) // Noncompliant
            .build()
        
        val config3 = RealmConfiguration.Builder()
            .name("noncompliant-S6301-realm.db")
            .encryptionKey(getKey3(s))
            .build()

        val config4 = RealmConfiguration.Builder()
            .name("noncompliant-S6301-realm.db")
            .encryptionKey(getKey4(s))
            .build()
    }


    fun sqlite() {
        val key = byteArrayOf(0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
            0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
            0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
            0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,
            0x00)

        val database2 = SQLiteDatabase("test.db", "foobar".toCharArray(), null, 0)  // Noncompliant

        val s = "foobar"
        val database1 = SQLiteDatabase.openOrCreateDatabase("test.db", s, null) // Noncompliant

        val s1 = "foobar".toCharArray()
        val database3 = SQLiteDatabase.openOrCreateDatabase("test.db", s1, null) // Noncompliant

        val s2 = charArrayOf()
        val database4 = SQLiteDatabase.openOrCreateDatabase("test.db", s2, null) // Noncompliant {{The "password" parameter should not be hardcoded.}}
//                                                                     ^^

        val database5 = SQLiteDatabase.openOrCreateDatabase("test.db", s.toCharArray(), null) // Noncompliant {{The "password" parameter should not be hardcoded.}}
//                                                                     ^^^^^^^^^^^^^^^

        val database6 = SQLiteDatabase.openOrCreateDatabase("test.db", getKey(), null) // Noncompliant {{The "password" parameter should not be hardcoded.}}
//                                                                     ^^^^^^^^

        val database7 = SQLiteDatabase.openOrCreateDatabase("test.db", k.key(), null)

        val database8 = SQLiteDatabase.openOrCreateDatabase("test.db", this.getKey(), null) // Noncompliant {{The "password" parameter should not be hardcoded.}}

        val database9 = SQLiteDatabase.openOrCreateDatabase(
            "test.db",
            this.getKey(),  // Noncompliant {{The "password" parameter should not be hardcoded.}}
//          ^^^^^^^^^^^^^
            null
        )
    }

    private fun getKey(): ByteArray {
        return byteArrayOf(
            0,
            1,
            2,
        )
    }
    
    private fun getKey2() = byteArrayOf(
            0,
            1,
            2,
        )
    
    private fun getKey3(s: String) = s.toByteArray()

    private fun getKey4(s: String): ByteArray {
        return s.toByteArray()
    }
}

abstract class Key {
    abstract fun key() : String
}
