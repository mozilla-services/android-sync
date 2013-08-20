/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.crypto;

import java.security.GeneralSecurityException;

/**
 * Wrapper to perform PBKDF2-HMAC-SHA-256 using libcrypto from OpenSSL.
 */
public class NativeCrypto {
    static {
      System.loadLibrary("crypto");
      System.loadLibrary("nativecrypto");
    }

    public native static byte[] pbkdf2SHA256(byte[] password, byte[] salt, int c, int dkLen);

    /**
     * The following functions are purloined from SCrypt.java.
     */

    /**
     * Implementation of the <a href="http://www.tarsnap.com/scrypt/scrypt.pdf"/>scrypt KDF</a>.
     * Calls the native implementation {@link #scryptN} when the native library was successfully
     * loaded, otherwise calls {@link #scryptJ}.
     *
     * @param passwd    Password.
     * @param salt      Salt.
     * @param N         CPU cost parameter.
     * @param r         Memory cost parameter.
     * @param p         Parallelization parameter.
     * @param dkLen     Intended length of the derived key.
     *
     * @return The derived key.
     *
     * @throws GeneralSecurityException when HMAC_SHA256 is not available.
     */
    public static byte[] scrypt(byte[] passwd, byte[] salt, int N, int r, int p, int dkLen) throws GeneralSecurityException {
        return scryptN(passwd, salt, N, r, p, dkLen);
    }

    /**
     * Native C implementation of the <a href="http://www.tarsnap.com/scrypt/scrypt.pdf"/>scrypt KDF</a> using
     * the code from <a href="http://www.tarsnap.com/scrypt.html">http://www.tarsnap.com/scrypt.html<a>.
     *
     * @param passwd    Password.
     * @param salt      Salt.
     * @param N         CPU cost parameter.
     * @param r         Memory cost parameter.
     * @param p         Parallelization parameter.
     * @param dkLen     Intended length of the derived key.
     *
     * @return The derived key.
     */
    public static native byte[] scryptN(byte[] passwd, byte[] salt, int N, int r, int p, int dkLen);

}
