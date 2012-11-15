package org.mozilla.gecko.jwcrypto;

import java.security.PublicKey;

/**
 * BrowserID certificates are signed versions including a printable
 * representation of a public key. <code>PublicKeySerializer</code> instances
 * generate such printable representations.
 */
public interface PublicKeySerializer {
  /**
   * Generate a printable representation of a public key.
   *
   * @param publicKey to represent.
   * @return printable representation.
   */
  public String serializePublicKey(PublicKey publicKey);
}
