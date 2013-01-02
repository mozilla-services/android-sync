package org.mozilla.gecko.jwcrypto;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.RSAPublicKeySpec;

import org.mozilla.gecko.sync.ExtendedJSONObject;

public class RSACryptoImplementation implements JWCryptoImplementation, PublicKeySerializer {
  @Override
  public KeyPair generateKeypair(final int keysize) throws NoSuchAlgorithmException {
    final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
    keyPairGenerator.initialize(keysize);
    final KeyPair keyPair = keyPairGenerator.generateKeyPair();
    return keyPair;
  }

  @Override
  public String serializePublicKey(PublicKey publicKey) {
    RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;

    ExtendedJSONObject o = new ExtendedJSONObject();
    o.put("algorithm", "RS"); // Hard-coded, but this is all RSA.
    o.put("n", rsaPublicKey.getModulus().toString(10));
    o.put("e", rsaPublicKey.getPublicExponent().toString(10));

    return o.toJSONString();
  }

  @Override
  public byte[] signMessage(byte[] bytes, PrivateKey privateKey)
      throws GeneralSecurityException {
    final Signature signer = Signature.getInstance("SHA256withRSA");
    signer.initSign(privateKey);
    signer.update(bytes);
    return signer.sign();
  }

  @Override
  public boolean verifyMessage(byte[] bytes, byte[] signature, PublicKey publicKey)
      throws GeneralSecurityException {
    final Signature signer = Signature.getInstance("SHA256withRSA");
    signer.initVerify(publicKey);
    signer.update(bytes);
    return signer.verify(signature);
  }

  public PrivateKey createPrivateKey(BigInteger n, BigInteger d) throws NoSuchAlgorithmException, InvalidKeySpecException {
    if (n == null) {
      throw new IllegalArgumentException("n must not be null");
    }
    if (d == null) {
      throw new IllegalArgumentException("d must not be null");
    }

    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    KeySpec keySpec = new RSAPrivateKeySpec(n, d);
    PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

    return privateKey;
  }

  public PublicKey createPublicKey(BigInteger n, BigInteger e) throws NoSuchAlgorithmException, InvalidKeySpecException {
    if (n == null) {
      throw new IllegalArgumentException("n must not be null");
    }
    if (e == null) {
      throw new IllegalArgumentException("e must not be null");
    }

    KeyFactory keyFactory = KeyFactory.getInstance("RSA");
    KeySpec keySpec = new RSAPublicKeySpec(n, e);
    PublicKey publicKey = keyFactory.generatePublic(keySpec);

    return publicKey;
  }
}
