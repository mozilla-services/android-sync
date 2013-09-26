/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.fxa.crypto;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import org.mozilla.gecko.sync.ExtendedJSONObject;

public interface KeyGenerator {
  public abstract KeyPair generateKeypair(int keysize)
      throws NoSuchAlgorithmException;
  public ExtendedJSONObject serializePublicKey(PublicKey publicKey);
}
