package android.webkit;

public abstract class WebSettings {
  public static final int MIXED_CONTENT_ALWAYS_ALLOW = 0;
  public static final int MIXED_CONTENT_NEVER_ALLOW = 1;
  public static final int MIXED_CONTENT_COMPATIBILITY_MODE = 2;

  public abstract int getMixedContentMode();
  public abstract void setMixedContentMode(int mode);

  public abstract boolean getJavaScriptEnabled();
  public abstract void setJavaScriptEnabled(boolean flag);

  public abstract boolean getAllowFileAccess();
  public abstract void setAllowFileAccess(boolean allow);

  public abstract boolean getAllowContentAccess();
  public abstract void setAllowContentAccess(boolean allow);

  public abstract boolean getAllowFileAccessFromFileURLs();
  @Deprecated
  public abstract void setAllowFileAccessFromFileURLs(boolean flag);

  public abstract boolean getAllowUniversalAccessFromFileURLs();
  @Deprecated
  public abstract void setAllowUniversalAccessFromFileURLs(boolean flag);

}
