/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;

import org.json.simple.parser.ParseException;
import org.junit.Test;
import org.mozilla.android.sync.CollectionKeys;
import org.mozilla.android.sync.CryptoRecord;
import org.mozilla.android.sync.NoCollectionKeysSetException;
import org.mozilla.android.sync.NonObjectJSONException;
import org.mozilla.android.sync.crypto.CryptoException;
import org.mozilla.android.sync.crypto.Cryptographer;
import org.mozilla.android.sync.crypto.KeyBundle;

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
    return; // TODO

    /*
    rec.encrypt();
    // TODO: this isn't working.

    CollectionKeys ck = new CollectionKeys();
    ck.setKeyPairsFromWBO(rec, syncKeyBundle);
    byte[] input = "3fI6k1exImMgAKjilmMaAWxGqEIzFX/9K5EjEgH99vc=".getBytes("UTF-8");
    byte[] expected = Base64.decodeBase64(input);
    assertSame(expected, ck.defaultKeyBundle().getEncryptionKey());
    */
  }
}
