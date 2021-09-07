package checks

import android.content.Context
import android.os.Environment;

class ExternalAndroidStorageAccessCheckSample {
    fun environmentNoncompliant(env: Environment) {
        env.getExternalStoragePublicDirectory("foo") // Noncompliant {{Make sure accessing the Android external storage is safe here.}}
//          ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        env.getExternalStorageDirectory() // Noncompliant
    }

    fun environmentCompliant(env: Environment) {
        env.getDownloadCacheDirectory() // ok
    }

    fun contextNoncompliant(ctx: Context, cc: ContextChild) {
        ctx.getExternalFilesDir("") // Noncompliant
        ctx.getExternalFilesDirs("") // Noncompliant
        ctx.externalCacheDir // Noncompliant
//          ^^^^^^^^^^^^^^^^
        ctx.getExternalCacheDir() // Noncompliant
        ctx.externalCacheDirs // Noncompliant
        ctx.getExternalCacheDirs() // Noncompliant
        ctx.externalMediaDirs // Noncompliant
        ctx.getExternalMediaDirs() // Noncompliant
        ctx.obbDir // Noncompliant
        ctx.getObbDir() // Noncompliant
        ctx.obbDirs // Noncompliant
        ctx.getObbDirs() // Noncompliant

        cc.getExternalFilesDir("") // Noncompliant
        cc.getExternalFilesDirs("") // Noncompliant
        cc.externalCacheDir // Noncompliant
        cc.getExternalCacheDir() // Noncompliant
        cc.externalCacheDirs // Noncompliant
        cc.getExternalCacheDirs() // Noncompliant
        cc.externalMediaDirs // Noncompliant
        cc.getExternalMediaDirs() // Noncompliant
        cc.obbDir // Noncompliant
        cc.getObbDir() // Noncompliant
        cc.obbDirs // Noncompliant
        cc.getObbDirs() // Noncompliant
    }

    fun contextCompliant(ctx: Context) {
        ctx.compliantDir
        ctx.getCompliantDir()
    }
}

class ContextChild: Context()
