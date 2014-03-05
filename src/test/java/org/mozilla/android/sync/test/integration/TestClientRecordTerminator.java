/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test.integration;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URI;

import org.json.simple.JSONArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mozilla.android.sync.test.integration.TestBasicFetch.LiveDelegate;
import org.mozilla.gecko.background.testhelpers.MockSharedPreferences;
import org.mozilla.gecko.sync.CryptoRecord;
import org.mozilla.gecko.sync.ExtendedJSONObject;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.config.ClientRecordTerminator;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.repositories.domain.FormHistoryRecord;

import android.content.SharedPreferences;

@Category(IntegrationTestCategory.class)
public class TestClientRecordTerminator extends TestWithTokenHelper {
  protected static final String TEST_USERNAME = "test2";

  protected KeyBundle syncKeyBundle;
  protected SharedPreferences sharedPrefs;
  protected SyncConfiguration config;

  @Override
  protected String getMockMyIDUserName() {
    return TestClientRecordTerminator.class.getSimpleName();
  }

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();

    sharedPrefs = new MockSharedPreferences();
    syncKeyBundle = KeyBundle.withRandomKeys();
    config = new SyncConfiguration(TEST_USERNAME, authHeaderProvider, sharedPrefs, syncKeyBundle);
    config.clusterURL = new URI(token.endpoint);
  }

  @Test
  public void testDeleteClientRecord() throws Exception {
    final String COLLECTION = "clients";
    final String COLLECTION_URL = config.collectionURI(COLLECTION).toString();

    final String[] RECS = new String[] { "keep1", "kill1", "keep2", "kill2" }; // Records to be PUT to server.
    final String[] KEEP = new String[] { "keep1", "keep2" }; // Records to leave on server.
    final String[] KILL = new String[] { "kill1", "kill2" }; // Records to delete from server.

    // Put records -- doesn't matter what type of record.  This overwrites anything already on the server.
    for (String guid : RECS) {
      final FormHistoryRecord record = new FormHistoryRecord(guid, COLLECTION);
      record.fieldName  = "testFieldName";
      record.fieldValue = "testFieldValue";
      CryptoRecord rec = record.getEnvelope();
      rec.setKeyBundle(syncKeyBundle);
      rec.encrypt();
      final String RECORD_URL = config.wboURI(COLLECTION, guid).toString();
      LiveDelegate ld = TestBasicFetch.realLivePut(authHeaderProvider, RECORD_URL, rec);
      assertNotNull(ld.body());
    }

    // Make sure record appears in collection guids.
    JSONArray a = ExtendedJSONObject.parseJSONArray(TestBasicFetch.realLiveFetch(authHeaderProvider, COLLECTION_URL).body());
    for (String guid : RECS) {
      assertTrue(a.contains(guid));
    }

    SyncConfiguration configuration = new SyncConfiguration(TEST_USERNAME, authHeaderProvider, new MockSharedPreferences());
    configuration.clusterURL = new URI(token.endpoint);
    for (String guid : KILL) {
      ClientRecordTerminator.deleteClientRecord(configuration, guid);
    }

    // Make sure record does not appear in collection guids.
    a = ExtendedJSONObject.parseJSONArray(TestBasicFetch.realLiveFetch(authHeaderProvider, COLLECTION_URL).body());
    for (String guid : KEEP) {
      assertTrue(a.contains(guid));
    }
    for (String guid : KILL) {
      assertFalse(a.contains(guid));
    }
  }
}
