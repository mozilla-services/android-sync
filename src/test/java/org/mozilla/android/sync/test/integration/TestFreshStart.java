/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.integration;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.mozilla.android.sync.test.helpers.MockGlobalSessionCallback;
import org.mozilla.android.sync.test.helpers.MockPrefsGlobalSession;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.CollectionKeys;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.InfoCollections;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.MetaGlobal;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.delegates.FreshStartDelegate;

public class TestFreshStart {
  // TODO: switch this to use a local server, with appropriate setup.
  static final String TEST_CLUSTER_URL  = "https://scl2-sync1283.services.mozilla.com/";
  static final String TEST_ACCOUNT      = "nalexander+test0425@mozilla.com";
  static final String TEST_USERNAME     = "6gnkjphdltbntwnrgvu46ey6mu7ncjdl";
  static final String TEST_PASSWORD     = "test0425";
  static final String TEST_USER_PASS    = TEST_USERNAME + ":" + TEST_PASSWORD;
  static final String TEST_SYNC_KEY     = "fuyx96ea8rkfazvjdfuqumupye"; // Weave.Identity.syncKey

  private CollectionKeys keysToUpload;
  private KeyBundle syncKeyBundle;
  private MockGlobalSessionCallback callback;
  private GlobalSession session;

  @Before
  public void setUp()
      throws IllegalStateException, NonObjectJSONException, IOException,
      ParseException, CryptoException, SyncConfigurationException, IllegalArgumentException, URISyntaxException {
    Logger.LOG_TO_STDOUT = true;

    keysToUpload = CollectionKeys.generateCollectionKeys();
    keysToUpload.setKeyBundleForCollection("addons", KeyBundle.withRandomKeys());
    syncKeyBundle = new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY);

    callback = new MockGlobalSessionCallback();
    session = new MockPrefsGlobalSession(SyncConfiguration.DEFAULT_USER_API, TEST_CLUSTER_URL, TEST_USERNAME, TEST_PASSWORD, null,
        syncKeyBundle, callback, null, null, null) {
      @Override
      public CollectionKeys generateNewCryptoKeys() {
        return keysToUpload;
      }

      // On fresh start completed, just stop.
      @Override
      public void freshStart() {
        freshStart(this, new FreshStartDelegate() {
          @Override
          public void onFreshStartFailed(Exception e) {
            WaitHelper.getTestWaiter().performNotify(e);
          }

          @Override
          public void onFreshStart() {
            WaitHelper.getTestWaiter().performNotify();
          }
        });
      }
    };
    session.config.clusterURL = new URI(TEST_CLUSTER_URL);
  }

  protected void doFreshStart() {
    WaitHelper.getTestWaiter().performWait(WaitHelper.onThreadRunnable(new Runnable() {
      public void run() {
        try {
          session.freshStart();
        } catch (Exception e) {
          WaitHelper.getTestWaiter().performNotify(e);
        }
      }
    }));
  }

  @Test
  public void testLiveFreshStart() throws Exception {
    assertEquals(TEST_USERNAME, KeyBundle.usernameFromAccount(TEST_ACCOUNT));
    session.config.enabledEngineNames = new HashSet<String>();
    session.config.enabledEngineNames.add("bookmarks");
    session.config.enabledEngineNames.add("clients");
    session.config.enabledEngineNames.add("addons");
    session.config.enabledEngineNames.add("prefs");

    doFreshStart();

    // Verify that meta and crypto are the only entries in info/collections.
    ExtendedJSONObject o = TestBasicFetch.realLiveFetch(TEST_USERNAME, TEST_PASSWORD, session.config.infoURL()).jsonObject();
    InfoCollections infoCollections = new InfoCollections(null, null);
    infoCollections.setFromRecord(o);
    assertNotNull(infoCollections.getTimestamp("meta"));
    assertNotNull(infoCollections.getTimestamp("crypto"));
    assertEquals(2, o.object.entrySet().size());

    // Verify that meta/global looks okay.
    o = TestBasicFetch.realLiveFetch(TEST_USERNAME, TEST_PASSWORD, session.config.metaURL()).jsonObject();
    assertNotNull(o);
    MetaGlobal mg = new MetaGlobal(null, null);
    mg.setFromRecord(CryptoRecord.fromJSONRecord(o));
    assertEquals(new Long(GlobalSession.STORAGE_VERSION), mg.getStorageVersion());
    List<String> namesList = new ArrayList<String>(mg.getEnabledEngineNames());
    Collections.sort(namesList);
    String[] names = namesList.toArray(new String[namesList.size()]);
    String[] expected = new String[] { "addons", "bookmarks", "clients", "prefs" };
    assertArrayEquals(expected, names);
    assertEquals(GlobalSession.BOOKMARKS_ENGINE_VERSION, mg.getEngines().getObject("bookmarks").getIntegerSafely("version").intValue());
    assertEquals(GlobalSession.CLIENTS_ENGINE_VERSION, mg.getEngines().getObject("clients").getIntegerSafely("version").intValue());
    assertEquals(0, mg.getEngines().getObject("addons").getIntegerSafely("version").intValue());
    assertEquals(0, mg.getEngines().getObject("prefs").getIntegerSafely("version").intValue());

    // Verify that crypto/keys looks okay.
    String jsonCryptoKeys = TestBasicFetch.realLiveFetch(TEST_USERNAME, TEST_PASSWORD, session.config.keysURI().toString()).body();
    CollectionKeys keys = new CollectionKeys();
    keys.setKeyPairsFromWBO(CryptoRecord.fromJSONRecord(jsonCryptoKeys), syncKeyBundle);
    assertTrue(keys.equals(keysToUpload));
    assertTrue(keys.keyBundleForCollectionIsNotDefault("addons"));
    assertFalse(keys.keyBundleForCollectionIsNotDefault("bookmarks"));
  }
}
