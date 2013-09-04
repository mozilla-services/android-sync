/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.fxa;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import org.json.simple.JSONObject;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.PBKDF2;
import org.mozilla.gecko.sync.crypto.Scrypt;

public class PICLClientSideKeyStretcher implements ClientSideKeyStretcher {
  protected final PBKDF2 pbkdf2;
  protected final Scrypt scrypt;

  public PICLClientSideKeyStretcher(PBKDF2 pbkdf2, Scrypt scrypt) {
    if (pbkdf2 == null) {
      throw new IllegalArgumentException("pbkdf2 must not be null");
    }
    if (scrypt == null) {
      throw new IllegalArgumentException("scrypt must not be null");
    }
    this.pbkdf2 = pbkdf2;
    this.scrypt = scrypt;
  }

  /**
   * Stretch a user entered password, following
   * <a href="https://wiki.mozilla.org/Identity/AttachedServices/KeyServerProtocol#Client-Side_Key_Stretching">https://wiki.mozilla.org/Identity/AttachedServices/KeyServerProtocol#Client-Side_Key_Stretching</a>.
   */
  public byte[] stretch(String email, String password, JSONObject options) throws UnsupportedEncodingException, GeneralSecurityException {
    if (email == null) {
      throw new IllegalArgumentException("email must not be null");
    }
    if (password == null) {
      throw new IllegalArgumentException("password must not be null.");
    }

    if (options != null) {
      throw new IllegalArgumentException("No options accepted.");
    }

    byte[] emailUTF8 = email.getBytes("UTF-8");
    byte[] passwordUTF8 = password.getBytes("UTF-8");

    byte[] k1 = pbkdf2.pbkdf2SHA256(passwordUTF8, FxAccountUtils.KWE("first-PBKDF", emailUTF8), 20*1000, 1*32);
    byte[] k2 = scrypt.scrypt(k1, FxAccountUtils.KW("scrypt"), 64*1024, 8, 1, 1*32);

    byte[] in = Utils.concatAll(k2, passwordUTF8);
    byte[] stretchedPW = pbkdf2.pbkdf2SHA256(in, FxAccountUtils.KWE("second-PBKDF", emailUTF8), 20*1000, 1*32);

    return stretchedPW;
  }
}
