/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.crypto;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.Mac;

import org.mozilla.apache.commons.codec.binary.Base64;

/*
 * All info in these objects should be decoded (i.e. not BaseXX encoded).
 */
public class CryptoInfo {

  private byte[] message;
  private byte[] iv;
  private byte[] hmac;
  private KeyBundle keys;

  /*
   * Constructor typically used when encrypting.
   */
  public CryptoInfo(byte[] message, KeyBundle keys) {
    this.setMessage(message);
    this.setKeys(keys);
  }

  /*
   * Constructor typically used when decrypting.
   */
  public CryptoInfo(byte[] message, byte[] iv, byte[] hmac, KeyBundle keys) {
    this.setMessage(message);
    this.setIV(iv);
    this.setHMAC(hmac);
    this.setKeys(keys);
  }

  public byte[] getMessage() {
    return message;
  }

  public void setMessage(byte[] message) {
    this.message = message;
  }

  public byte[] getIV() {
    return iv;
  }

  public void setIV(byte[] iv) {
    this.iv = iv;
  }

  public byte[] getHMAC() {
    return hmac;
  }

  public void setHMAC(byte[] hmac) {
    this.hmac = hmac;
  }

  public KeyBundle getKeys() {
    return keys;
  }

  public void setKeys(KeyBundle keys) {
    this.keys = keys;
  }

  /*
   * Generate HMAC for given cipher text.
   */
  public byte[] generatedHMAC() throws NoSuchAlgorithmException, InvalidKeyException {
    Mac hmacHasher = HKDF.makeHMACHasher(getKeys().getHMACKey());
    return hmacHasher.doFinal(Base64.encodeBase64(getMessage()));
  }

  /*
   * Return true if generated HMAC is the same as the specified HMAC.
   */
  public boolean generatedHMACIsHMAC() throws NoSuchAlgorithmException, InvalidKeyException {
    byte[] generatedHMAC = generatedHMAC();
    byte[] expectedHMAC  = getHMAC();
    return Arrays.equals(generatedHMAC, expectedHMAC);
  }
}