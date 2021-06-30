package android.security.keystore;


import android.annotation.NonNull;
import org.jetbrains.annotations.NotNull;

public final class KeyGenParameterSpec {
  
  public final static class Builder {
   
    public Builder(@NonNull String keystoreAlias, int purposes) { }

    @NonNull
    public Builder setBlockModes(@NonNull String... blockModeGcm) {
      return this;
    }

    @NotNull
    public Builder setEncryptionPaddings(@NotNull String... paddings) {
      return this;
    }

    @NotNull
    public Builder setUserAuthenticationRequired(boolean b) {
      return this;
    }

    @NotNull
    public KeyGenParameterSpec build() {
      return null;
    }
  }
}
