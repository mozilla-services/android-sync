/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.fxa;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import org.json.simple.JSONObject;

public interface ClientSideKeyStretcher {
  /**
   * Stretch a user entered password.
   *
   * @param email associated with account.
   * @param password to stretch.
   * @param options algorithm specific options.
   * @return stretched password.
   * @throws UnsupportedEncodingException when the email or password cannot be converted to UTF-8 bytes (should never happen).
   * @throws GeneralSecurityException when the hashing or cryptography fails entirely.
   */
  public byte[] stretch(String email, String password, JSONObject options) throws UnsupportedEncodingException, GeneralSecurityException;
}
