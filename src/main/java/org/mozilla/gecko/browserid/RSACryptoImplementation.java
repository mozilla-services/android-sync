/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.browserid;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

import org.mozilla.gecko.sync.ExtendedJSONObject;

public class RSACryptoImplementation {
  public static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

  protected static class RSAPublicKey implements PublicKey {
    protected final java.security.interfaces.RSAPublicKey publicKey;

    public RSAPublicKey(java.security.interfaces.RSAPublicKey publicKey) {
      this.publicKey = publicKey;
    }

    @Override
    public String serialize() {
      ExtendedJSONObject o = new ExtendedJSONObject();
      o.put("algorithm", "RS");
      o.put("n", publicKey.getModulus().toString(10));
      o.put("e", publicKey.getPublicExponent().toString(10));
      return o.toJSONString();
    }

    @Override
    public boolean verifyMessage(byte[] bytes, byte[] signature)
        throws GeneralSecurityException {
      final Signature signer = Signature.getInstance(SIGNATURE_ALGORITHM);
      signer.initVerify(publicKey);
      signer.update(bytes);
      return signer.verify(signature);
    }
  }

  protected static class RSAPrivateKey implements PrivateKey {
    protected final java.security.interfaces.RSAPrivateKey privateKey;

    public RSAPrivateKey(java.security.interfaces.RSAPrivateKey privateKey) {
      this.privateKey = privateKey;
    }

    @Override
    public String getAlgorithm() {
      return "RS" + (privateKey.getModulus().bitLength() + 7)/8;
    }

    @Override
    public String serialize() {
      ExtendedJSONObject o = new ExtendedJSONObject();
      o.put("algorithm", "RS");
      o.put("n", privateKey.getModulus().toString(10));
      o.put("e", privateKey.getPrivateExponent().toString(10));
      return o.toJSONString();
    }

    @Override
    public byte[] signMessage(byte[] bytes)
        throws GeneralSecurityException {
      final Signature signer = Signature.getInstance(SIGNATURE_ALGORITHM);
      signer.initSign(privateKey);
      signer.update(bytes);
      return signer.sign();
    }
  }

  public static KeyPair generateKeypair(final int keysize) throws NoSuchAlgorithmException {
    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(keysize);
    final java.security.KeyPair keyPair = keyPairGenerator.generateKeyPair();
    java.security.interfaces.RSAPrivateKey privateKey = (java.security.interfaces.RSAPrivateKey) keyPair.getPrivate();
    java.security.interfaces.RSAPublicKey publicKey = (java.security.interfaces.RSAPublicKey) keyPair.getPublic();
    return new KeyPair(new RSAPrivateKey(privateKey), new RSAPublicKey(publicKey));
  }

  public static PrivateKey createPrivateKey(BigInteger n, BigInteger d) throws NoSuchAlgorithmException, InvalidKeySpecException {
    if (n == null) {
      throw new IllegalArgumentException("n must not be null");
    }
    if (d == null) {
      throw new IllegalArgumentException("d must not be null");
    }
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    KeySpec keySpec = new RSAPrivateKeySpec(n, d);
    java.security.interfaces.RSAPrivateKey privateKey = (java.security.interfaces.RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    return new RSAPrivateKey(privateKey);
  }

  public static PublicKey createPublicKey(BigInteger n, BigInteger e) throws NoSuchAlgorithmException, InvalidKeySpecException {
    if (n == null) {
      throw new IllegalArgumentException("n must not be null");
    }
    if (e == null) {
      throw new IllegalArgumentException("e must not be null");
    }
    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    KeySpec keySpec = new RSAPublicKeySpec(n, e);
    java.security.interfaces.RSAPublicKey publicKey = (java.security.interfaces.RSAPublicKey) keyFactory.generatePublic(keySpec);
    return new RSAPublicKey(publicKey);
  }
}
