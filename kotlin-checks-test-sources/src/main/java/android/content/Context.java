package android.content;

import java.io.File;
import net.sqlcipher.database.CursorFactory;
import net.sqlcipher.database.DatabaseErrorHandler;
import net.sqlcipher.database.SQLiteDatabase;

public class Context {
  public File getExternalFilesDir(String type) {
    return null;
  }

  public File[] getExternalFilesDirs(String type) {
    return null;
  }

  public File getExternalCacheDir() {
    return null;
  }

  public File[] getExternalCacheDirs() {
    return null;
  }

  public File[] getExternalMediaDirs() {
    return null;
  }

  public File getObbDir() {
    return null;
  }

  public File[] getObbDirs() {
    return null;
  }

  public File getCompliantDir() {
    return null;
  }

  public SharedPreferences getSharedPreferences(String name, int mode) {
    return null;
  }

  public SharedPreferences getSharedPreferences(File file, int mode) {
    return null;
  }

  public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory) {
    return null;
  }

  public SQLiteDatabase openOrCreateDatabase(String name, int mode, CursorFactory factory, DatabaseErrorHandler errorHandler) {
    return null;
  }
}
