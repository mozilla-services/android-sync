/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.mozilla.apache.commons.codec.binary.Base64;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;

public class TestCryptoRecord {
  String base64EncryptionKey = "9K/wLdXdw+nrTtXo4ZpECyHFNr4d7aYHqeg3KW9+m6Q=";
  String base64HmacKey = "MMntEfutgLTc8FlTLQFms8/xMPmCldqPlq/QQXEjx70=";

  @Test
  public void testBaseCryptoRecordEncrypt() throws IOException, ParseException, NonObjectJSONException, CryptoException {
    ExtendedJSONObject clearPayload = ExtendedJSONObject.parseJSONObject("{\"id\":\"5qRsgXWRJZXr\",\"title\":\"Index of file:///Users/jason/Library/Application Support/Firefox/Profiles/ksgd7wpk.LocalSyncServer/weave/logs/\",\"histUri\":\"file:///Users/jason/Library/Application%20Support/Firefox/Profiles/ksgd7wpk.LocalSyncServer/weave/logs/\",\"visits\":[{\"type\":1,\"date\":1319149012372425}]}");

    CryptoRecord record = new CryptoRecord();
    record.payload = clearPayload;
    String expectedGUID = "5qRsgXWRJZXr";
    record.guid = expectedGUID;
    record.keyBundle = KeyBundle.decodeKeyStrings(base64EncryptionKey, base64HmacKey);
    record.encrypt();
    assertTrue(record.payload.get("title") == null);
    assertTrue(record.payload.get("ciphertext") != null);
    assertEquals(expectedGUID, record.guid);
    assertEquals(expectedGUID, record.toJSONObject().get("id"));
    System.out.println("Encrypted JSON: " + record.toJSONString());
    record.decrypt();
    System.out.println("Decrypted JSON: " + record.toJSONString());
    assertEquals(expectedGUID, record.toJSONObject().get("id"));
   // assertEquals(record.payload, clearPayload);
  }

  @Test
  public void testBaseCryptoRecordDecrypt() throws CryptoException,
                                           IOException, ParseException,
                                           NonObjectJSONException {
    String base64CipherText =
          "NMsdnRulLwQsVcwxKW9XwaUe7ouJk5Wn"
        + "80QhbD80l0HEcZGCynh45qIbeYBik0lg"
        + "cHbKmlIxTJNwU+OeqipN+/j7MqhjKOGI"
        + "lvbpiPQQLC6/ffF2vbzL0nzMUuSyvaQz"
        + "yGGkSYM2xUFt06aNivoQTvU2GgGmUK6M"
        + "vadoY38hhW2LCMkoZcNfgCqJ26lO1O0s"
        + "EO6zHsk3IVz6vsKiJ2Hq6VCo7hu123wN"
        + "egmujHWQSGyf8JeudZjKzfi0OFRRvvm4"
        + "QAKyBWf0MgrW1F8SFDnVfkq8amCB7Nhd"
        + "whgLWbN+21NitNwWYknoEWe1m6hmGZDg"
        + "DT32uxzWxCV8QqqrpH/ZggViEr9uMgoy"
        + "4lYaWqP7G5WKvvechc62aqnsNEYhH26A"
        + "5QgzmlNyvB+KPFvPsYzxDnSCjOoRSLx7"
        + "GG86wT59QZw=";
    String base64IV = "GX8L37AAb2FZJMzIoXlX8w==";
    String base16Hmac = 
          "b1e6c18ac30deb70236bc0d65a46f7a4"
        + "dce3b8b0e02cf92182b914e3afa5eebc";

    ExtendedJSONObject body    = new ExtendedJSONObject();
    ExtendedJSONObject payload = new ExtendedJSONObject();
    payload.put("ciphertext", base64CipherText);
    payload.put("IV", base64IV);
    payload.put("hmac", base16Hmac);
    body.put("payload", payload.toJSONString());
    CryptoRecord record = CryptoRecord.fromJSONRecord(body);
    byte[] decodedKey  = Base64.decodeBase64(base64EncryptionKey.getBytes("UTF-8"));
    byte[] decodedHMAC = Base64.decodeBase64(base64HmacKey.getBytes("UTF-8")); 
    record.keyBundle = new KeyBundle(decodedKey, decodedHMAC);

    record.decrypt();
    System.out.println(record.payload);
    String id = (String) record.payload.get("id");
    assertTrue(id.equals("5qRsgXWRJZXr"));
  }

  @Test
  public void testBaseCryptoRecordSyncKeyBundle() throws UnsupportedEncodingException {
    // These values pulled straight out of Firefox.
    String key  = "6m8mv8ex2brqnrmsb9fjuvfg7y";
    String user = "c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd";
    
    // Check our friendly base32 decoding.
    System.out.println("Decodes to " + new String(Base64.encodeBase64(Utils.decodeFriendlyBase32(key))));
    System.out.println("Expecting  " + "8xbKrJfQYwbFkguKmlSm/g==");
    assertTrue(Arrays.equals(Utils.decodeFriendlyBase32(key), Base64.decodeBase64("8xbKrJfQYwbFkguKmlSm/g==".getBytes("UTF-8"))));
    KeyBundle bundle = new KeyBundle(user, key);
    String expectedEncryptKeyBase64 = "/8RzbFT396htpZu5rwgIg2WKfyARgm7dLzsF5pwrVz8=";
    String expectedHMACKeyBase64    = "NChGjrqoXYyw8vIYP2334cvmMtsjAMUZNqFwV2LGNkM=";
    byte[] computedEncryptKey       = bundle.getEncryptionKey();
    byte[] computedHMACKey          = bundle.getHMACKey();
    System.out.println("Got encryption key:      " + new String(Base64.encodeBase64(computedEncryptKey)));
    System.out.println("Expected encryption key: " + expectedEncryptKeyBase64);
    assertTrue(Arrays.equals(computedEncryptKey, Base64.decodeBase64(expectedEncryptKeyBase64.getBytes("UTF-8"))));
    assertTrue(Arrays.equals(computedHMACKey,    Base64.decodeBase64(expectedHMACKeyBase64.getBytes("UTF-8"))));
  }
}
