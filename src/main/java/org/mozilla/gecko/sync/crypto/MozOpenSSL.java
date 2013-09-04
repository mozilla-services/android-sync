/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.crypto;

/**
 * Wrapper to perform PBKDF2-HMAC-SHA-256 using libcrypto from OpenSSL.
 */
public class MozOpenSSL {
    static {
      System.loadLibrary("crypto");
      System.loadLibrary("mozopenssl");
    }

    public native static byte[] pbkdf2SHA256(byte[] password, byte[] salt, int c, int dkLen);
}
