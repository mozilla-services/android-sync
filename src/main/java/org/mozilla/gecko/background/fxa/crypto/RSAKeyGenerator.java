/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.fxa.crypto;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;

import org.mozilla.gecko.sync.ExtendedJSONObject;

public class RSAKeyGenerator implements KeyGenerator {
  @Override
  public KeyPair generateKeypair(final int keysize) throws NoSuchAlgorithmException {
    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(keysize);
    final KeyPair keyPair = keyPairGenerator.generateKeyPair();
    return keyPair;
  }

  @Override
  public ExtendedJSONObject serializePublicKey(PublicKey publicKey) {
    RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;

    ExtendedJSONObject o = new ExtendedJSONObject();
    o.put("algorithm", "RS"); // Hard-coded, but this is all RSA.
    o.put("n", rsaPublicKey.getModulus().toString(10));
    o.put("e", rsaPublicKey.getPublicExponent().toString(10));

    return o;
  }
}
