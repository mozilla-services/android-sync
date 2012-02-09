/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;

import org.mozilla.apache.commons.codec.binary.Base64;
import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.Cryptographer;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.NonObjectJSONException;

public class TestCollectionKeys {

  @Test
  public void testDefaultKeys() throws CryptoException, NoCollectionKeysSetException {
    CollectionKeys ck = new CollectionKeys();
    try {
      ck.defaultKeyBundle();
      fail("defaultKeys should throw.");
    } catch (NoCollectionKeysSetException ex) {
      // Good.
    }
    KeyBundle testKeys = Cryptographer.generateKeys();
    ck.setDefaultKeyBundle(testKeys);
    assertEquals(testKeys, ck.defaultKeyBundle());
  }

  @Test
  public void testKeyForCollection() throws CryptoException, NoCollectionKeysSetException {
    CollectionKeys ck = new CollectionKeys();
    try {
      ck.keyBundleForCollection("test");
      fail("keyForCollection should throw.");
    } catch (NoCollectionKeysSetException ex) {
      // Good.
    }
    KeyBundle testKeys = Cryptographer.generateKeys();
    KeyBundle otherKeys = Cryptographer.generateKeys();

    ck.setDefaultKeyBundle(testKeys);
    assertEquals(testKeys, ck.defaultKeyBundle());
    assertEquals(testKeys, ck.keyBundleForCollection("test"));  // Returns default.

    ck.setKeyBundleForCollection("test", otherKeys);
    assertEquals(otherKeys, ck.keyBundleForCollection("test"));  // Returns default.

  }

  public static void assertSame(byte[] arrayOne, byte[] arrayTwo) {
    assertTrue(Arrays.equals(arrayOne, arrayTwo));
  }


  @Test
  public void testSetKeysFromWBO() throws IOException, ParseException, NonObjectJSONException, CryptoException, NoCollectionKeysSetException {
    String json = "{\"default\":[\"3fI6k1exImMgAKjilmMaAWxGqEIzFX/9K5EjEgH99vc=\",\"/AMaoCX4hzic28WY94XtokNi7N4T0nv+moS1y5wlbug=\"],\"collections\":{},\"collection\":\"crypto\",\"id\":\"keys\"}";
    CryptoRecord rec = new CryptoRecord(json);

    KeyBundle syncKeyBundle = new KeyBundle("slyjcrjednxd6rf4cr63vqilmkus6zbe", "6m8mv8ex2brqnrmsb9fjuvfg7y");
    rec.keyBundle = syncKeyBundle;

    rec.encrypt();
    CollectionKeys ck = new CollectionKeys();
    ck.setKeyPairsFromWBO(rec, syncKeyBundle);
    byte[] input = "3fI6k1exImMgAKjilmMaAWxGqEIzFX/9K5EjEgH99vc=".getBytes("UTF-8");
    byte[] expected = Base64.decodeBase64(input);
    assertSame(expected, ck.defaultKeyBundle().getEncryptionKey());
  }

  @Test
  public void testCryptoRecordFromCollectionKeys() throws CryptoException, NoCollectionKeysSetException, IOException, ParseException, NonObjectJSONException {
    CollectionKeys ck1 = CollectionKeys.generateCollectionKeys();
    assertNotNull(ck1.defaultKeyBundle());
    assertEquals(ck1.keyBundleForCollection("foobar"), ck1.defaultKeyBundle());
    CryptoRecord rec = ck1.asCryptoRecord();
    assertEquals(rec.collection, "crypto");
    assertEquals(rec.guid, "keys");
    JSONArray defaultKey = (JSONArray) rec.payload.get("default");

    assertSame(Base64.decodeBase64((String) (defaultKey.get(0))), ck1.defaultKeyBundle().getEncryptionKey());
    CollectionKeys ck2 = CollectionKeys.fromCryptoRecord(rec, null);
    assertSame(ck1.defaultKeyBundle().getEncryptionKey(), ck2.defaultKeyBundle().getEncryptionKey());
  }
}
