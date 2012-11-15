package org.mozilla.gecko.jwcrypto;

import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

public interface JWCryptoImplementation {
  public abstract KeyPair generateKeypair(int keysize)
      throws NoSuchAlgorithmException;

  public abstract byte[] signMessage(byte[] bytes, PrivateKey privateKey)
      throws GeneralSecurityException;

  public abstract boolean verifyMessage(byte[] bytes, byte[] signature, PublicKey publicKey)
      throws GeneralSecurityException;
}
