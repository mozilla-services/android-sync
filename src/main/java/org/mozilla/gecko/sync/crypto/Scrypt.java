/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.crypto;

import java.security.GeneralSecurityException;

public interface Scrypt {
  public byte[] scrypt(byte[] password, byte[] salt, int N, int r, int p, int dkLen) throws GeneralSecurityException;
}
