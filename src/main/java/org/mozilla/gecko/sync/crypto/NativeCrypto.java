/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.crypto;

import java.security.GeneralSecurityException;

public class NativeCrypto {
  static {
    System.loadLibrary("androidcrypto");
  }

  /**
   * Wrapper to perform PBKDF2-HMAC-SHA-256 in native code.
   */
  public native static byte[] pbkdf2SHA256(byte[] password, byte[] salt, int c, int dkLen)
      throws GeneralSecurityException;
}
