package android.app;

import android.content.Context;
import android.content.SharedPreferences;

public class Activity extends Context {
  public SharedPreferences getPreferences(int mode) {
    return new SharedPreferences();
  }
}
