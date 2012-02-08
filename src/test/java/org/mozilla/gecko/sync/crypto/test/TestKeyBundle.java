package org.mozilla.gecko.sync.crypto.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import org.junit.Test;
import org.mozilla.apache.commons.codec.binary.Base64;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;

public class TestKeyBundle {
  @Test
  public void testUsernameFromAccount() throws NoSuchAlgorithmException, UnsupportedEncodingException {
    assertEquals(Utils.sha1Base32("foobar@baz.com"), "xee7ffonluzpdp66l6xgpyh2v2w6ojkc");
    assertEquals(KeyBundle.usernameFromAccount("foobar@baz.com"), "xee7ffonluzpdp66l6xgpyh2v2w6ojkc");
    assertEquals(KeyBundle.usernameFromAccount("foobar"), "foobar");
    assertEquals(KeyBundle.usernameFromAccount("xee7ffonluzpdp66l6xgpyh2v2w6ojkc"), "xee7ffonluzpdp66l6xgpyh2v2w6ojkc");
  }

  @Test
  public void testCreateKeyBundle() throws UnsupportedEncodingException, CryptoException {
    String username              = "smqvooxj664hmrkrv6bw4r4vkegjhkns";
    String friendlyBase32SyncKey = "gbh7teqqcgyzd65svjgibd7tqy";
    String base64EncryptionKey   = "069EnS3EtDK4y1tZ1AyKX+U7WEsWRp9b" +
                                   "RIKLdW/7aoE=";
    String base64HmacKey         = "LF2YCS1QCgSNCf0BCQvQ06SGH8jqJDi9" +
                                   "dKj0O+b0fwI=";

    KeyBundle keys = new KeyBundle(username, friendlyBase32SyncKey);
    assertTrue(Arrays.equals(keys.getEncryptionKey(), Base64.decodeBase64(base64EncryptionKey.getBytes("UTF-8"))));
    assertTrue(Arrays.equals(keys.getHMACKey(), Base64.decodeBase64(base64HmacKey.getBytes("UTF-8"))));
  }

  /*
   * Basic sanity check to make sure length of keys is correct (32 bytes).
   * Also make sure that the two keys are different.
   */
  @Test
  public void testGenerateRandomKeys() throws CryptoException {
    KeyBundle keys = KeyBundle.withRandomKeys();

    assertEquals(keys.getEncryptionKey().length, 32);
    assertEquals(keys.getHMACKey().length, 32);

    boolean equal = Arrays.equals(keys.getEncryptionKey(), keys.getHMACKey());
    assertEquals(false, equal);
  }
}
