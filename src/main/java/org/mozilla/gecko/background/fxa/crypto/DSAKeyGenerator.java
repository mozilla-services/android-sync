/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.fxa.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;

import org.mozilla.gecko.sync.ExtendedJSONObject;

public class DSAKeyGenerator implements KeyGenerator {
  @Override
  public KeyPair generateKeypair(final int keysize) throws NoSuchAlgorithmException {
    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DSA");
    keyPairGenerator.initialize(keysize);
    final KeyPair keyPair = keyPairGenerator.generateKeyPair();
    return keyPair;
  }

  @Override
  public ExtendedJSONObject serializePublicKey(PublicKey publicKey) {
    DSAPublicKey dsaPublicKey = (DSAPublicKey) publicKey;
    DSAParams params = dsaPublicKey.getParams();

    ExtendedJSONObject o = new ExtendedJSONObject();
    o.put("algorithm", "DS"); // Hard-coded, but this is all DSA.
    o.put("y", dsaPublicKey.getY().toString(10));
    o.put("g", params.getG().toString(10));
    o.put("p", params.getP().toString(10));
    o.put("q", params.getQ().toString(10));

    return o;
  }
}
