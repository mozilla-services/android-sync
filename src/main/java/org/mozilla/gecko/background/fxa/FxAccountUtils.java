/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.fxa;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.mozilla.gecko.sync.Utils;

public class FxAccountUtils {
  public static byte[] KW(String name) throws UnsupportedEncodingException {
    return Utils.concatAll(
        "identity.mozilla.com/picl/v1/".getBytes("UTF-8"),
        name.getBytes("UTF-8"));
  }

  public static byte[] KWE(String name, byte[] emailUTF8) throws UnsupportedEncodingException {
    return Utils.concatAll(
        "identity.mozilla.com/picl/v1/".getBytes("UTF-8"),
        name.getBytes("UTF-8"),
        ":".getBytes("UTF-8"),
        emailUTF8);
  }

  public static byte[] sha256(byte[] in) throws NoSuchAlgorithmException {
    MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
    return sha256.digest(in);
  }
}
