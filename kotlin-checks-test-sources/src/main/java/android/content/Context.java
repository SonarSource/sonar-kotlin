package android.content;

import java.io.File;

import android.os.Bundle;
import android.os.Handler;
import android.os.UserHandle;
import net.sqlcipher.database.CursorFactory;
import net.sqlcipher.database.DatabaseErrorHandler;
import net.sqlcipher.database.SQLiteDatabase;
import org.jetbrains.annotations.Nullable;

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

  public void sendBroadcast(Intent intent) {

  }

  public void sendBroadcast(Intent intent, String broadcastPermission) {

  }

  public void sendOrderedBroadcastAsUser(@Nullable Intent intent, @Nullable UserHandle user, @Nullable String broadcastPermission, @Nullable BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {

  }

  public void sendOrderedBroadcast(@Nullable Intent intent, @Nullable String broadcastPermission) {

  }

  public void sendBroadcastAsUser(@Nullable Intent intent, @Nullable UserHandle user, @Nullable String broadcastPermission) {

  }

  public void sendBroadcastAsUser(@Nullable Intent intent, @Nullable UserHandle user) {

  }

  public void sendStickyOrderedBroadcastAsUser(@Nullable Intent intent, @Nullable UserHandle user, @Nullable BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {

  }

  public void sendStickyBroadcast(@Nullable Intent intent) {

  }

  public void sendStickyBroadcastAsUser(@Nullable Intent intent, @Nullable UserHandle user) {

  }

  public void sendStickyOrderedBroadcast(@Nullable Intent intent, @Nullable BroadcastReceiver resultReceiver, @Nullable Handler scheduler, int initialCode, @Nullable String initialData, @Nullable Bundle initialExtras) {

  }

  public void registerReceiver(@Nullable BroadcastReceiver receiver, @Nullable IntentFilter filter) {

  }

  public void registerReceiver(@Nullable BroadcastReceiver receiver, @Nullable IntentFilter filter, int flags) {

  }

  public void registerReceiver(@Nullable BroadcastReceiver receiver, @Nullable IntentFilter filter, @Nullable String permissions, @Nullable Handler scheduler) {

  }

  public void registerReceiver(@Nullable BroadcastReceiver receiver, @Nullable IntentFilter filter, @Nullable String permissions, @Nullable Handler scheduler, int flags) {

  }
}
