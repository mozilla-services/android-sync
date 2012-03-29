package org.mozilla.gecko.sync.crypto.test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.MockSharedPreferences;
import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.NoCollectionKeysSetException;
import org.mozilla.gecko.sync.PrefsSource;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;

import android.content.SharedPreferences;

public class TestPersistedCrypto5Keys implements PrefsSource {
  MockSharedPreferences prefs = null;

  @Override
  public SharedPreferences getPrefs(String name, int mode) {
    if (prefs == null) {
      prefs = new MockSharedPreferences();
    }
    return prefs;
  }

  @Before
  public void setUp() {
    Logger.LOG_TO_STDOUT = true;
    prefs = null;
  }

  @Test
  public void testPersistLastModified() throws CryptoException, NoCollectionKeysSetException {
    long LAST_MODIFIED = System.currentTimeMillis();
    SyncConfiguration sc = new SyncConfiguration("sync.prefs.testEnsureKeysStage", this);
    sc.syncKeyBundle = KeyBundle.withRandomKeys();

    // Test fresh start.
    assertEquals(-1, sc.persistedCryptoKeys().lastModified());

    // Test persisting.
    sc.persistedCryptoKeys().persistLastModified(LAST_MODIFIED);
    assertEquals(LAST_MODIFIED, sc.persistedCryptoKeys().lastModified());

    // Test clearing.
    sc.persistedCryptoKeys().persistLastModified(0);
    assertEquals(-1, sc.persistedCryptoKeys().lastModified());
  }

  @Test
  public void testPersistKeys() throws CryptoException, NoCollectionKeysSetException {
    KeyBundle syncKeyBundle = KeyBundle.withRandomKeys();
    KeyBundle testKeyBundle = KeyBundle.withRandomKeys();

    SyncConfiguration sc = new SyncConfiguration("sync.prefs.testEnsureKeysStage", this);
    sc.syncKeyBundle = syncKeyBundle;

    // Test fresh start.
    assertNull(sc.persistedCryptoKeys().keys());

    // Test persisting.
    CollectionKeys keys = new CollectionKeys();
    keys.setDefaultKeyBundle(syncKeyBundle);
    keys.setKeyBundleForCollection("test", testKeyBundle);
    sc.persistedCryptoKeys().persistKeys(keys);

    CollectionKeys persistedKeys = sc.persistedCryptoKeys().keys();
    assertNotNull(persistedKeys);
    assertArrayEquals(syncKeyBundle.getEncryptionKey(), persistedKeys.defaultKeyBundle().getEncryptionKey());
    assertArrayEquals(syncKeyBundle.getHMACKey(), persistedKeys.defaultKeyBundle().getHMACKey());
    assertArrayEquals(testKeyBundle.getEncryptionKey(), persistedKeys.keyBundleForCollection("test").getEncryptionKey());
    assertArrayEquals(testKeyBundle.getHMACKey(), persistedKeys.keyBundleForCollection("test").getHMACKey());

    // Test clearing.
    sc.persistedCryptoKeys().persistKeys(null);
    assertNull(sc.persistedCryptoKeys().keys());

    // Test loading a persisted bundle with wrong syncKeyBundle.
    sc.persistedCryptoKeys().persistKeys(keys);
    assertNotNull(sc.persistedCryptoKeys().keys());

    sc = new SyncConfiguration("sync.prefs.testEnsureKeysStage", this); // Need new SyncConfiguration.
    sc.syncKeyBundle = testKeyBundle;
    assertNull(sc.persistedCryptoKeys().keys());
  }
}
