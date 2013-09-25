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
import java.security.interfaces.DSAParams;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.Utils;

public class DSACryptoImplementation {
  public static final String SIGNATURE_ALGORITHM = "SHA1withDSA";
  public static final int SIGNATURE_LENGTH = 40; // DSA signatures are always length 40.

  protected static class DSAPublicKey implements PublicKey {
    protected final java.security.interfaces.DSAPublicKey publicKey;

    public DSAPublicKey(java.security.interfaces.DSAPublicKey publicKey) {
      this.publicKey = publicKey;
    }

    @Override
    public String serialize() {
      DSAParams params = publicKey.getParams();
      ExtendedJSONObject o = new ExtendedJSONObject();
      o.put("algorithm", "DS");
      o.put("y", publicKey.getY().toString(16));
      o.put("g", params.getG().toString(16));
      o.put("p", params.getP().toString(16));
      o.put("q", params.getQ().toString(16));
      return o.toJSONString();
    }

    @Override
    public boolean verifyMessage(byte[] bytes, byte[] signature)
        throws GeneralSecurityException {
      if (bytes == null) {
        throw new IllegalArgumentException("bytes must not be null");
      }
      if (signature == null) {
        throw new IllegalArgumentException("signature must not be null");
      }
      if (signature.length != SIGNATURE_LENGTH) {
        return false;
      }
      byte[] first = new byte[signature.length / 2];
      byte[] second = new byte[signature.length / 2];
      System.arraycopy(signature, 0, first, 0, first.length);
      System.arraycopy(signature, first.length, second, 0, second.length);
      BigInteger r = new BigInteger(Utils.byte2hex(first), 16);
      BigInteger s = new BigInteger(Utils.byte2hex(second), 16);
      byte[] encoded = ASNUtils.encodeTwoArraysToASN1(Utils.hex2Byte(r.toString(16)), Utils.hex2Byte(s.toString(16)));

      final Signature signer = Signature.getInstance(SIGNATURE_ALGORITHM);
      signer.initVerify(publicKey);
      signer.update(bytes);
      return signer.verify(encoded);
    }
  }

  protected static class DSAPrivateKey implements PrivateKey {
    protected final java.security.interfaces.DSAPrivateKey privateKey;

    public DSAPrivateKey(java.security.interfaces.DSAPrivateKey privateKey) {
      this.privateKey = privateKey;
    }

    @Override
    public String getAlgorithm() {
      return "DS" + (privateKey.getParams().getP().bitLength() + 7)/8;
    }

    @Override
    public String serialize() {
      DSAParams params = privateKey.getParams();
      ExtendedJSONObject o = new ExtendedJSONObject();
      o.put("algorithm", "DS");
      o.put("x", privateKey.getX().toString(16));
      o.put("g", params.getG().toString(16));
      o.put("p", params.getP().toString(16));
      o.put("q", params.getQ().toString(16));
      return o.toJSONString();
    }

    @Override
    public byte[] signMessage(byte[] bytes)
        throws GeneralSecurityException {
      if (bytes == null) {
        throw new IllegalArgumentException("bytes must not be null");
      }
      final Signature signer = Signature.getInstance(SIGNATURE_ALGORITHM);
      signer.initSign(privateKey);
      signer.update(bytes);
      final byte[] signature = signer.sign();

      final byte[][] arrays = ASNUtils.decodeTwoArraysFromASN1(signature);
      BigInteger r = new BigInteger(arrays[0]);
      BigInteger s = new BigInteger(arrays[1]);
      // This is awful, but signatures are always length 40.
      return Utils.concatAll(Utils.hex2Byte(r.toString(16), SIGNATURE_LENGTH / 2), Utils.hex2Byte(s.toString(16), SIGNATURE_LENGTH / 2));
    }
  }

  public static KeyPair generateKeypair(int keysize)
      throws NoSuchAlgorithmException {
    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DSA");
    keyPairGenerator.initialize(keysize);
    final java.security.KeyPair keyPair = keyPairGenerator.generateKeyPair();
    java.security.interfaces.DSAPrivateKey privateKey = (java.security.interfaces.DSAPrivateKey) keyPair.getPrivate();
    java.security.interfaces.DSAPublicKey publicKey = (java.security.interfaces.DSAPublicKey) keyPair.getPublic();
    return new KeyPair(new DSAPrivateKey(privateKey), new DSAPublicKey(publicKey));
  }

  public static PrivateKey createPrivateKey(BigInteger x, BigInteger p, BigInteger q, BigInteger g) throws NoSuchAlgorithmException, InvalidKeySpecException {
    if (x == null) {
      throw new IllegalArgumentException("x must not be null");
    }
    if (p == null) {
      throw new IllegalArgumentException("p must not be null");
    }
    if (q == null) {
      throw new IllegalArgumentException("q must not be null");
    }
    if (g == null) {
      throw new IllegalArgumentException("g must not be null");
    }
    KeySpec keySpec = new DSAPrivateKeySpec(x, p, q, g);
    KeyFactory keyFactory = KeyFactory.getInstance("DSA");
    java.security.interfaces.DSAPrivateKey privateKey = (java.security.interfaces.DSAPrivateKey) keyFactory.generatePrivate(keySpec);
    return new DSAPrivateKey(privateKey);
  }

  public static PublicKey createPublicKey(BigInteger y, BigInteger p, BigInteger q, BigInteger g) throws NoSuchAlgorithmException, InvalidKeySpecException {
    if (y == null) {
      throw new IllegalArgumentException("n must not be null");
    }
    if (p == null) {
      throw new IllegalArgumentException("p must not be null");
    }
    if (q == null) {
      throw new IllegalArgumentException("q must not be null");
    }
    if (g == null) {
      throw new IllegalArgumentException("g must not be null");
    }
    KeySpec keySpec = new DSAPublicKeySpec(y, p, q, g);
    KeyFactory keyFactory = KeyFactory.getInstance("DSA");
    java.security.interfaces.DSAPublicKey publicKey = (java.security.interfaces.DSAPublicKey) keyFactory.generatePublic(keySpec);
    return new DSAPublicKey(publicKey);
  }
}
