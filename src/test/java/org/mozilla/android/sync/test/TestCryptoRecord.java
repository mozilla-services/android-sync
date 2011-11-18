package org.mozilla.android.sync.test;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.mozilla.android.sync.BaseCryptoRecord;
import org.mozilla.android.sync.ExtendedJSONObject;
import org.mozilla.android.sync.NonObjectJSONException;
import org.mozilla.android.sync.crypto.CryptoException;
import org.mozilla.android.sync.crypto.KeyBundle;
import org.mozilla.android.sync.crypto.Utils;

public class TestCryptoRecord {

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
    String base64EncryptionKey = "9K/wLdXdw+nrTtXo4ZpECyHFNr4d7aYHqeg3KW9+m6Q=";
    String base64HmacKey = "MMntEfutgLTc8FlTLQFms8/xMPmCldqPlq/QQXEjx70=";

    ExtendedJSONObject body    = new ExtendedJSONObject();
    ExtendedJSONObject payload = new ExtendedJSONObject();
    payload.put("ciphertext", base64CipherText);
    payload.put("IV", base64IV);
    payload.put("hmac", base16Hmac);
    body.put("payload", payload.toJSONString());
    BaseCryptoRecord record = new BaseCryptoRecord(body);
    record.keyBundle = new KeyBundle(Base64.decodeBase64(base64EncryptionKey),
        Base64.decodeBase64(base64HmacKey));
    record.decrypt();
    System.out.println(record.cleartext);
    String id = (String) record.cleartext.getObject("payload").get("id");
    assertTrue(id.equals("5qRsgXWRJZXr"));
  }

  @Test
  public void testBaseCryptoRecordSyncKeyBundle() {
    // These values pulled straight out of Firefox.
    String key  = "6m8mv8ex2brqnrmsb9fjuvfg7y";
    String user = "c6o7dvmr2c4ud2fyv6woz2u4zi22bcyd";
    
    // Check our friendly base32 decoding.
    System.out.println("Decodes to " + Base64.encodeBase64String(Utils.decodeFriendlyBase32(key)));
    System.out.println("Expecting  " + "8xbKrJfQYwbFkguKmlSm/g==");
    assertTrue(Arrays.equals(Utils.decodeFriendlyBase32(key), Base64.decodeBase64("8xbKrJfQYwbFkguKmlSm/g==")));
    KeyBundle bundle = new KeyBundle(user, key);
    String expectedEncryptKeyBase64 = "/8RzbFT396htpZu5rwgIg2WKfyARgm7dLzsF5pwrVz8=";
    String expectedHMACKeyBase64    = "NChGjrqoXYyw8vIYP2334cvmMtsjAMUZNqFwV2LGNkM=";
    byte[] computedEncryptKey       = bundle.getEncryptionKey();
    byte[] computedHMACKey          = bundle.getHMACKey();
    System.out.println("Got encryption key:      " + Base64.encodeBase64String(computedEncryptKey));
    System.out.println("Expected encryption key: " + expectedEncryptKeyBase64);
    assertTrue(Arrays.equals(computedEncryptKey, Base64.decodeBase64(expectedEncryptKeyBase64)));
    assertTrue(Arrays.equals(computedHMACKey,    Base64.decodeBase64(expectedHMACKeyBase64)));
  }
}
