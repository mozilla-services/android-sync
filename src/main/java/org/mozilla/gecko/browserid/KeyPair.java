/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.browserid;

public class KeyPair {
  public final PrivateKey privateKey;
  public final PublicKey publicKey;

  public KeyPair(PrivateKey privateKey, PublicKey publicKey) {
    this.privateKey = privateKey;
    this.publicKey = publicKey;
  }

  public PrivateKey getPrivate() {
    return this.privateKey;
  }

  public PublicKey getPublic() {
    return this.publicKey;
  }
}
