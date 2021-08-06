package checks

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import io.realm.RealmConfiguration
import net.sqlcipher.database.CursorFactory
import net.sqlcipher.database.DatabaseErrorHandler
import java.io.File

class UnencryptedDatabaseOnMobileCheckSample {
    open class MyActivity : Activity()
    class MyActivity2a : MyActivity()
    class MyActivity2b : MyActivity() {
        override fun getPreferences(mode: Int): SharedPreferences? {
            // do something safe
            return null
        }
    }
    class MyContext : Context()
    class FakeActivity {
        fun getPreferences(i: Int) {}
    }

    fun test() {
        Activity().getPreferences(1) // Noncompliant {{Make sure using an unencrypted database is safe here.}}
        MyActivity().getPreferences(2) // Noncompliant
        MyActivity2a().getPreferences(4) // Noncompliant
        val activityA = MyActivity2a()
        activityA.getPreferences(8) // Noncompliant

        // FP
        MyActivity2b().getPreferences(0) // Noncompliant

        val activityB = MyActivity2b()
        // FP
        activityB.getPreferences(0) // Noncompliant

        val activityC: Activity = MyActivity2b()
        // FP, as we don't know that activityC is always of type MyActivity2b. That's ok.
        activityC.getPreferences(0) // Noncompliant

        Context().getSharedPreferences(File(""), 0) // Noncompliant
        Context().getSharedPreferences("", 0) // Noncompliant
        MyContext().getSharedPreferences(File(""), 0) // Noncompliant
        MyContext().getSharedPreferences("", 0) // Noncompliant

        PreferenceManager.getDefaultSharedPreferences(Context()) // Noncompliant

        Context().openOrCreateDatabase("", 0, CursorFactory()) // Noncompliant
        Context().openOrCreateDatabase("", 0, CursorFactory(), DatabaseErrorHandler()) // Noncompliant

        FakeActivity().getPreferences(0) // Ok

        RealmConfiguration.Builder()
            .build() // Noncompliant

        RealmConfiguration.Builder()
            .name("")
            .name("")
            .name("")
            .build() // Noncompliant

        RealmConfiguration.Builder()
            .encryptionKey("".toByteArray())
            .build() // Ok
    }
}
