/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.crypto;

public interface PBKDF2 {
  public byte[] pbkdf2SHA1(byte[] p, byte[] s, int c, int dkLen);
  public byte[] pbkdf2SHA256(byte[] p, byte[] s, int c, int dkLen);
}
