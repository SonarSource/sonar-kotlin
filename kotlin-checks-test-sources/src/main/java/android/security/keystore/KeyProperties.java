package android.security.keystore;

public class KeyProperties {

  /**
   * Purpose of key: encryption.
   */
  public static final int PURPOSE_ENCRYPT = 1;

  /**
   * Purpose of key: decryption.
   */
  public static final int PURPOSE_DECRYPT = 1 << 1;

  /** Galois/Counter Mode (GCM) block mode. */
  public static final String BLOCK_MODE_GCM = "GCM";

  /**
   * No encryption padding.
   */
  public static final String ENCRYPTION_PADDING_NONE = "NoPadding";

  public static final String KEY_ALGORITHM_DES = "DES";
  public static final String KEY_ALGORITHM_3DES = "DESede";
  public static final String BLOCK_MODE_CBC = "CBC";
  public static final String BLOCK_MODE_ECB = "ECB";
  public static final String ENCRYPTION_PADDING_PKCS7 = "PKCS7Padding";
}
