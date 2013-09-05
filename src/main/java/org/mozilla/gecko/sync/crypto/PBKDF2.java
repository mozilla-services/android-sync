/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.crypto;

import java.security.GeneralSecurityException;

public interface PBKDF2 {
  public byte[] pbkdf2SHA1(byte[] password, byte[] salt, int c, int dkLenInBytes) throws GeneralSecurityException;
  public byte[] pbkdf2SHA256(byte[] password, byte[] salt, int c, int dkLenInBytes) throws GeneralSecurityException;
}
