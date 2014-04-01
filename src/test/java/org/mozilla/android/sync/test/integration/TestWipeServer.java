/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.json.simple.JSONArray;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mozilla.android.sync.test.helpers.MockGlobalSessionCallback;
import org.mozilla.android.sync.test.integration.TestBasicFetch.LiveDelegate;
import org.mozilla.gecko.background.testhelpers.MockPrefsGlobalSession;
import org.mozilla.gecko.background.testhelpers.MockSharedPreferences;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.NonObjectJSONException;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.SyncConfigurationException;
import org.mozilla.gecko.sync.crypto.CryptoException;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.net.BasicAuthHeaderProvider;
import org.mozilla.gecko.sync.repositories.domain.FormHistoryRecord;
import org.mozilla.gecko.sync.stage.FormHistoryServerSyncStage;
import org.mozilla.gecko.sync.stage.ServerSyncStage;

import android.content.SharedPreferences;

@Category(IntegrationTestCategory.class)
public class TestWipeServer {
  // TODO: switch this to use a local server, with appropriate setup.
  static final String TEST_CLUSTER_URL  = "https://scl2-sync1283.services.mozilla.com/";
  static final String TEST_ACCOUNT      = "nalexander+test0425@mozilla.com";
  static final String TEST_USERNAME     = "6gnkjphdltbntwnrgvu46ey6mu7ncjdl";
  static final String TEST_PASSWORD     = "test0425";
  static final String TEST_SYNC_KEY     = "fuyx96ea8rkfazvjdfuqumupye"; // Weave.Identity.syncKey

  private KeyBundle syncKeyBundle;
  private MockGlobalSessionCallback callback;
  private GlobalSession session;

  @Before
  public void setUp()
      throws IllegalStateException, NonObjectJSONException, IOException,
      ParseException, CryptoException, SyncConfigurationException, IllegalArgumentException, URISyntaxException {

    syncKeyBundle = new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY);
    callback = new MockGlobalSessionCallback(TEST_CLUSTER_URL);
    final SharedPreferences prefs = new MockSharedPreferences();
    final SyncConfiguration config = new SyncConfiguration(TEST_USERNAME, new BasicAuthHeaderProvider(TEST_USERNAME, TEST_PASSWORD), prefs);
    config.syncKeyBundle = syncKeyBundle;
    session = new MockPrefsGlobalSession(config, callback, null, null);
    session.config.clusterURL = new URI(TEST_CLUSTER_URL);
  }

  @Test
  public void testWipeEngineOnServer() throws Exception {
    final String COLLECTION = "forms";
    final SyncConfiguration config = session.config;
    final String COLLECTION_URL = config.collectionURI(COLLECTION).toString();
    final String RECORD_URL = COLLECTION_URL + "/testGuid";

    // Put record.
    FormHistoryRecord record = new FormHistoryRecord("testGuid", COLLECTION);
    record.fieldName  = "testFieldName";
    record.fieldValue = "testFieldValue";
    CryptoRecord rec = record.getEnvelope();
    rec.setKeyBundle(syncKeyBundle);
    rec.encrypt();
    LiveDelegate ld = TestBasicFetch.realLivePut(TEST_USERNAME, TEST_PASSWORD, RECORD_URL, rec);
    assertNotNull(ld.body());

    // Make sure record appears in collection guids.
    JSONArray a = ExtendedJSONObject.parseJSONArray(TestBasicFetch.realLiveFetch(TEST_USERNAME, TEST_PASSWORD, COLLECTION_URL).body());
    assertTrue(a.contains(record.guid));

    // Make sure record is really there.
    ExtendedJSONObject o = TestBasicFetch.realLiveFetch(TEST_USERNAME, TEST_PASSWORD, RECORD_URL).decrypt(TEST_SYNC_KEY);
    assertEquals(record.fieldName,  o.getString("name"));
    assertEquals(record.fieldValue, o.getString("value"));

    // Wipe server engine only.
    ServerSyncStage stage = new FormHistoryServerSyncStage();
    stage.wipeServer(session); // Synchronous!

    // Make sure record does not appear in collection guids.
    a = ExtendedJSONObject.parseJSONArray(TestBasicFetch.realLiveFetch(TEST_USERNAME, TEST_PASSWORD, COLLECTION_URL).body());
    assertTrue(a.isEmpty());
  }
}
