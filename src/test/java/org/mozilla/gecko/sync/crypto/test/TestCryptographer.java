/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.gecko.sync.crypto.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.junit.Test;
import org.mozilla.apache.commons.codec.binary.Base64;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.CryptoInfo;
import org.mozilla.gecko.sync.crypto.Cryptographer;
import org.mozilla.gecko.sync.crypto.KeyBundle;

public class TestCryptographer {

  @Test
  public void testCreateKeyBundle() throws UnsupportedEncodingException, CryptoException {
    String username =
        "smqvooxj664hmrkrv6bw4r4vkegjhkns";
    String friendlyBase32SyncKey =
        "gbh7teqqcgyzd65svjgibd7tqy";
    String base64EncryptionKey =
        "069EnS3EtDK4y1tZ1AyKX+U7WEsWRp9b" +
        "RIKLdW/7aoE=";
    String base64HmacKey =
        "LF2YCS1QCgSNCf0BCQvQ06SGH8jqJDi9" +
        "dKj0O+b0fwI=";

    KeyBundle keys = new KeyBundle(username, friendlyBase32SyncKey);

    // Check Encryption Key
    assertTrue(Arrays.equals(keys.getEncryptionKey(), Base64.decodeBase64(base64EncryptionKey.getBytes("UTF-8"))));

    // Check HMAC Key
    assertTrue(Arrays.equals(keys.getHMACKey(), Base64.decodeBase64(base64HmacKey.getBytes("UTF-8"))));
  }

  @Test
  public void testDecrypt() throws CryptoException {
    String base64CipherText =
        "NMsdnRulLwQsVcwxKW9XwaUe7ouJk5Wn" +
        "80QhbD80l0HEcZGCynh45qIbeYBik0lg" +
        "cHbKmlIxTJNwU+OeqipN+/j7MqhjKOGI" +
        "lvbpiPQQLC6/ffF2vbzL0nzMUuSyvaQz" +
        "yGGkSYM2xUFt06aNivoQTvU2GgGmUK6M" +
        "vadoY38hhW2LCMkoZcNfgCqJ26lO1O0s" +
        "EO6zHsk3IVz6vsKiJ2Hq6VCo7hu123wN" +
        "egmujHWQSGyf8JeudZjKzfi0OFRRvvm4" +
        "QAKyBWf0MgrW1F8SFDnVfkq8amCB7Nhd" +
        "whgLWbN+21NitNwWYknoEWe1m6hmGZDg" +
        "DT32uxzWxCV8QqqrpH/ZggViEr9uMgoy" +
        "4lYaWqP7G5WKvvechc62aqnsNEYhH26A" +
        "5QgzmlNyvB+KPFvPsYzxDnSCjOoRSLx7" +
        "GG86wT59QZw=";
    String base64IV =
        "GX8L37AAb2FZJMzIoXlX8w==";
    String base16Hmac =
        "b1e6c18ac30deb70236bc0d65a46f7a4" +
        "dce3b8b0e02cf92182b914e3afa5eebc";
    String base64EncryptionKey =
        "9K/wLdXdw+nrTtXo4ZpECyHFNr4d7aYH" +
        "qeg3KW9+m6Q=";
    String base64HmacKey =
        "MMntEfutgLTc8FlTLQFms8/xMPmCldqP" +
        "lq/QQXEjx70=";
    String base64ExpectedBytes =
        "eyJpZCI6IjVxUnNnWFdSSlpYciIsImhp" +
        "c3RVcmkiOiJmaWxlOi8vL1VzZXJzL2ph" +
        "c29uL0xpYnJhcnkvQXBwbGljYXRpb24l" +
        "MjBTdXBwb3J0L0ZpcmVmb3gvUHJvZmls" +
        "ZXMva3NnZDd3cGsuTG9jYWxTeW5jU2Vy" +
        "dmVyL3dlYXZlL2xvZ3MvIiwidGl0bGUi" +
        "OiJJbmRleCBvZiBmaWxlOi8vL1VzZXJz" +
        "L2phc29uL0xpYnJhcnkvQXBwbGljYXRp" +
        "b24gU3VwcG9ydC9GaXJlZm94L1Byb2Zp" +
        "bGVzL2tzZ2Q3d3BrLkxvY2FsU3luY1Nl" +
        "cnZlci93ZWF2ZS9sb2dzLyIsInZpc2l0" +
        "cyI6W3siZGF0ZSI6MTMxOTE0OTAxMjM3" +
        "MjQyNSwidHlwZSI6MX1dfQ==";

    byte[] decodedBytes = Cryptographer.decrypt(
        new CryptoInfo(
            Base64.decodeBase64(base64CipherText),
            Base64.decodeBase64(base64IV),
            Utils.hex2Byte(base16Hmac),
            new KeyBundle(
                Base64.decodeBase64(base64EncryptionKey),
                Base64.decodeBase64(base64HmacKey))
            ));

    // Check result
    boolean equals = Arrays.equals(decodedBytes, Base64.decodeBase64(base64ExpectedBytes));
    assertEquals(true, equals);
  }

  @Test
  public void testEncryptDecrypt() throws CryptoException {
    String clearText =
        "This is some cleartext written on" +
        " Halloween 2011!";
    String base64EncryptionKey =
        "9K/wLdXdw+nrTtXo4ZpECyHFNr4d7aYH" +
        "qeg3KW9+m6Q=";
    String base64HmacKey =
        "MMntEfutgLTc8FlTLQFms8/xMPmCldqP" +
        "lq/QQXEjx70=";

    // Encrypt
    CryptoInfo encrypted = Cryptographer.encrypt(
        new CryptoInfo(
            clearText.getBytes(),
            "".getBytes(),
            "".getBytes(),
            new KeyBundle(
                Base64.decodeBase64(base64EncryptionKey),
                Base64.decodeBase64(base64HmacKey))
            ));

    // Decrypt
    byte[] decrypted = Cryptographer.decrypt(encrypted);

    // Check result
    boolean equals = Arrays.equals(clearText.getBytes(), decrypted);
    assertEquals(true, equals);
  }

  /*
   * Basic sanity check to make sure length of keys is correct (32 bytes).
   * Also make sure that the two keys are different.
   */
  @Test
  public void testGenerateRandomKeys() throws CryptoException {
    KeyBundle keys = Cryptographer.generateKeys();

    assertEquals(keys.getEncryptionKey().length, 32);
    assertEquals(keys.getHMACKey().length, 32);

    boolean equal = Arrays.equals(keys.getEncryptionKey(), keys.getHMACKey());
    assertEquals(false, equal);
  }
}
