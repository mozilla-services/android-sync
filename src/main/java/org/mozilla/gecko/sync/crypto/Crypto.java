/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.crypto;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Main crypto bundle class. Dispatches to native code where appropriate.
 */
public class Crypto implements PBKDF2, Scrypt {

  @Override
  public byte[] scrypt(byte[] password, byte[] salt, int N, int r, int p,
                       int dkLen) {
    return NativeCrypto.scrypt(password, salt, N, r, p, dkLen);
  }

  @Override
  public byte[] pbkdf2SHA1(byte[] password, byte[] salt, int c, int dkLen) throws GeneralSecurityException {
    // Screw you, Java.
    char[] pw = new char[password.length];
    for (int i = 0; i < password.length; ++i) {
      pw[i] = (char) password[i];
    }

    // Won't work on API level 8, but this is trivial.
    SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
    PBEKeySpec keySpec = new PBEKeySpec(pw, salt, c, dkLen * 8);
    SecretKey key = factory.generateSecret(keySpec);
    return key.getEncoded();
  }

  @Override
  public byte[] pbkdf2SHA256(byte[] password, byte[] salt, int c, int dkLen) {
    return NativeCrypto.pbkdf2SHA256(password, salt, c, dkLen);
  }
}
