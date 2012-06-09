/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.stage.test;

import org.json.simple.JSONArray;
import org.mozilla.android.sync.test.AndroidSyncTestCase;
import org.mozilla.android.sync.test.helpers.DefaultGlobalSessionCallback;
import org.mozilla.android.sync.test.helpers.MockClientsDataDelegate;
import org.mozilla.gecko.sync.GlobalSession;
import org.mozilla.gecko.sync.SyncConfiguration;
import org.mozilla.gecko.sync.crypto.KeyBundle;
import org.mozilla.gecko.sync.delegates.ClientsDataDelegate;
import org.mozilla.gecko.sync.delegates.GlobalSessionCallback;
import org.mozilla.gecko.sync.repositories.android.ClientsDatabaseAccessor;
import org.mozilla.gecko.sync.repositories.domain.ClientRecord;
import org.mozilla.gecko.sync.stage.SyncClientsEngineStage;

import android.content.Context;

public class TestClientsStage extends AndroidSyncTestCase {
  private static int          TEST_PORT        = 15325;
  private static final String TEST_CLUSTER_URL = "http://localhost:" + TEST_PORT;
  private static final String TEST_USERNAME    = "johndoe";
  private static final String TEST_PASSWORD    = "password";
  private static final String TEST_SYNC_KEY    = "abcdeabcdeabcdeabcdeabcdea";

  public void setUp() {
    ClientsDatabaseAccessor db = new ClientsDatabaseAccessor(getApplicationContext());
    db.wipeDB();
    db.close();
  }

  public void testWipeClearsClients() throws Exception {

    // Wiping clients is equivalent to a reset and dropping all local stored client records.
    // Resetting is defined as being the same as for other engines -- discard local
    // and remote timestamps, tracked failed records, and tracked records to fetch.

    final Context context = getApplicationContext();
    final ClientsDatabaseAccessor dataAccessor = new ClientsDatabaseAccessor(context);
    final GlobalSessionCallback callback = new DefaultGlobalSessionCallback();
    final ClientsDataDelegate delegate = new MockClientsDataDelegate();

    final GlobalSession session = new GlobalSession(
        SyncConfiguration.DEFAULT_USER_API,
        TEST_CLUSTER_URL,
        TEST_USERNAME, TEST_PASSWORD, null,
        new KeyBundle(TEST_USERNAME, TEST_SYNC_KEY),
        callback, context, null, delegate);

    SyncClientsEngineStage stage = new SyncClientsEngineStage(session) {

      @Override
      public synchronized ClientsDatabaseAccessor getClientsDatabaseAccessor() {
        if (db == null) {
          db = dataAccessor;
        }
        return db;
      }
    };

    String guid = "clientabcdef";
    long lastModified = System.currentTimeMillis();
    ClientRecord record = new ClientRecord(guid, "clients", lastModified , false);
    record.name = "John's Phone";
    record.type = "mobile";
    record.commands = new JSONArray();

    dataAccessor.store(record);
    assertEquals(1, dataAccessor.clientsCount());

    stage.wipeLocal();

    assertEquals(0, dataAccessor.clientsCount());
    assertEquals(0L, session.config.getPersistedServerClientRecordTimestamp());
    assertEquals(0, session.getClientsDelegate().getClientsCount());
  }
}
