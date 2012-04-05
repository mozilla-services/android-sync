/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.test.helpers.ExpectFetchDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFetchSinceDelegate;
import org.mozilla.android.sync.test.helpers.ExpectGuidsSinceDelegate;
import org.mozilla.android.sync.test.helpers.ExpectStoredDelegate;
import org.mozilla.android.sync.test.helpers.PasswordHelpers;
import org.mozilla.android.sync.test.helpers.SessionTestHelper;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.InactiveSessionException;
import org.mozilla.gecko.sync.repositories.NoStoreDelegateException;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.android.BrowserContractHelpers;
import org.mozilla.gecko.sync.repositories.android.PasswordsRepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.gecko.sync.repositories.domain.PasswordRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.Context;
import android.util.Log;

public class TestPasswordsRepository extends AndroidSyncTestCase {
  private final String NEW_PASSWORD1 = "password";
  private final String NEW_PASSWORD2 = "drowssap";

  @Override
  public void setUp() {
    wipe();
    assertTrue(WaitHelper.getTestWaiter().isIdle());
  }

  public void testFetchAll() {
    RepositorySession session = createAndBeginSession();
    Record[] expected = new Record[] { PasswordHelpers.createPassword1(),
                                       PasswordHelpers.createPassword2() };

    performWait(storeRunnable(session, expected[0]));
    performWait(storeRunnable(session, expected[1]));

    performWait(fetchAllRunnable(session, expected));
    cleanup(session);
  }

  public void testGuidsSinceReturnMultipleRecords() {
    RepositorySession session = createAndBeginSession();

    PasswordRecord record1 = PasswordHelpers.createPassword1();
    PasswordRecord record2 = PasswordHelpers.createPassword2();

    updatePassword(NEW_PASSWORD1, record1);
    long timestamp = updatePassword(NEW_PASSWORD2, record2);

    String[] expected = new String[] { record1.guid, record2.guid };

    Logger.info("guidsSinceMultiple", "Storing two records...");
    performWait(storeRunnable(session, record1));
    performWait(storeRunnable(session, record2));

    Logger.info("guidsSinceMultiple", "Getting guids since " + timestamp + "; expecting " + expected.length);
    performWait(guidsSinceRunnable(session, timestamp, expected));
    cleanup(session);
  }

  public void testGuidsSinceReturnNoRecords() {
    RepositorySession session = createAndBeginSession();

    //  Store 1 record in the past.
    performWait(storeRunnable(session, PasswordHelpers.createPassword1()));

    String[] expected = {};
    performWait(guidsSinceRunnable(session, System.currentTimeMillis() + 1000, expected));
    cleanup(session);
  }

  public void testFetchSinceOneRecord() {
    RepositorySession session = createAndBeginSession();

    // Passwords fetchSince checks timePasswordChanged, not insertion time.
    PasswordRecord record1 = PasswordHelpers.createPassword1();
    long timeModified1 = updatePassword(NEW_PASSWORD1, record1);
    Log.i("fetchSinceOneRecord", "Storing record1.");
    performWait(storeRunnable(session, record1));

    PasswordRecord record2 = PasswordHelpers.createPassword2();
    long timeModified2 = updatePassword(NEW_PASSWORD2, record2);
    Log.i("fetchSinceOneRecord", "Storing record2.");
    performWait(storeRunnable(session, record2));

    Log.i("fetchSinceOneRecord", "Fetching record 1.");
    String[] expectedOne = new String[] { record2.guid };
    performWait(fetchSinceRunnable(session, timeModified2 - 10, expectedOne));

    Log.i("fetchSinceOneRecord", "Fetching both, relying on inclusiveness.");
    String[] expectedBoth = new String[] { record1.guid, record2.guid };
    performWait(fetchSinceRunnable(session, timeModified1 - 10, expectedBoth));

    Log.i("fetchSinceOneRecord", "Done.");
    cleanup(session);
  }

  public void testFetchSinceReturnNoRecords() {
   RepositorySession session = createAndBeginSession();

    performWait(storeRunnable(session, PasswordHelpers.createPassword2()));

    long timestamp = System.currentTimeMillis();

    performWait(fetchSinceRunnable(session, timestamp + 2000, new String[] {}));
    cleanup(session);
  }

  public void testFetchOneRecordByGuid() {
    RepositorySession session = createAndBeginSession();
    Record record = PasswordHelpers.createPassword1();
    performWait(storeRunnable(session, record));
    performWait(storeRunnable(session, PasswordHelpers.createPassword2()));

    String[] guids = new String[] { record.guid };
    Record[] expected = new Record[] { record };
    performWait(fetchRunnable(session, guids, expected));
    cleanup(session);
  }

  public void testFetchMultipleRecordsByGuids() {
    RepositorySession session = createAndBeginSession();
    PasswordRecord record1 = PasswordHelpers.createPassword1();
    PasswordRecord record2 = PasswordHelpers.createPassword2();
    PasswordRecord record3 = PasswordHelpers.createPassword3();

    performWait(storeRunnable(session, record1));
    performWait(storeRunnable(session, record2));
    performWait(storeRunnable(session, record3));

    String[] guids = new String[] { record1.guid, record2.guid };
    Record[] expected = new Record[] { record1, record2 };
    performWait(fetchRunnable(session, guids, expected));
    cleanup(session);
  }

  public void testFetchNoRecordByGuid() {
    RepositorySession session = createAndBeginSession();
    Record record = PasswordHelpers.createPassword1();

    performWait(storeRunnable(session, record));
    performWait(fetchRunnable(session,
                              new String[] { Utils.generateGuid() },
                              new Record[] {}));
    cleanup(session);
  }

  public void testStore() {
    final RepositorySession session = createAndBeginSession();
    performWait(storeRunnable(session, PasswordHelpers.createPassword1()));
  }

  public void testRemoteNewerTimeStamp() {
    final RepositorySession session = createAndBeginSession();

    // Store updated local record.
    PasswordRecord local = PasswordHelpers.createPassword1();
    updatePassword(NEW_PASSWORD1, local, System.currentTimeMillis() - 1000);
    Log.d(LOG_TAG, "local.guid: " + local.guid);
    Log.d(LOG_TAG, "local: " + local);
    performWait(storeRunnable(session, local));

    // Sync a remote record version that is newer.
    PasswordRecord remote = PasswordHelpers.createPassword2();
    remote.guid = local.guid;
    updatePassword(NEW_PASSWORD2, remote);
    Log.d(LOG_TAG, "remote.guid: " + remote.guid);
    Log.d(LOG_TAG, "remote: " + remote);
    performWait(storeRunnable(session, remote));

    // Make a fetch, expecting only the newer (remote) record.
    performWait(fetchAllRunnable(session, new Record[] { remote }));
    cleanup(session);
  }

  public void testLocalNewerTimeStamp() {
    final RepositorySession session = createAndBeginSession();
    // Remote record updated before local record.
    PasswordRecord remote = PasswordHelpers.createPassword1();
    updatePassword(NEW_PASSWORD1, remote, System.currentTimeMillis() - 1000);

    // Store updated local record.
    PasswordRecord local = PasswordHelpers.createPassword2();
    updatePassword(NEW_PASSWORD2, local);
    performWait(storeRunnable(session, local));

    // Sync a remote record version that is older.
    remote.guid = local.guid;
    performWait(storeRunnable(session, remote));

    // Make a fetch, expecting only the newer (local) record.
    performWait(fetchAllRunnable(session, new Record[] { local }));
    cleanup(session);
  }

  /*
   * Store two records that are identical except for guid. Expect to find the
   * remote one after reconciling.
   */
  public void testStoreIdenticalExceptGuid() {
    RepositorySession session = createAndBeginSession();
    PasswordRecord record = PasswordHelpers.createPassword1();
    Log.d(LOG_TAG, "record1.guid: " + record.guid);
    Log.d(LOG_TAG, "record1: " + record);
    // Store record.
    performWait(storeRunnable(session, record));

    // Store same record, but with different guid.
    record.guid = Utils.generateGuid();
    Log.d(LOG_TAG, "record2.guid: " + record.guid);
    Log.d(LOG_TAG, "record2: " + record);
    performWait(storeRunnable(session, record));

    performWait(fetchAllRunnable(session, new Record[] { record }));
    cleanup(session);
  }

  // Helper methods.
  private RepositorySession createAndBeginSession() {
    return SessionTestHelper.createAndBeginSession(
        getApplicationContext(),
        getRepository());
  }

  public void wipe() {
    Context context = getApplicationContext();
    context.getContentResolver().delete(BrowserContractHelpers.PASSWORDS_CONTENT_URI, null, null);
    context.getContentResolver().delete(BrowserContractHelpers.DELETED_PASSWORDS_CONTENT_URI, null, null);
  }

  private void cleanup(RepositorySession session) {
    if (session != null) {
      session.abort();
    }
  }

  private Repository getRepository() {
    /**
     * Override this chain in order to avoid our test code having to create two
     * sessions all the time. Don't track records, so they filtering doesn't happen.
     */
    return new PasswordsRepositorySession.PasswordsRepository() {
      @Override
      public void createSession(RepositorySessionCreationDelegate delegate,
          Context context) {
        PasswordsRepositorySession session;
        session = new PasswordsRepositorySession(this, context) {
          @Override
          protected synchronized void trackRecord(Record record) {
            System.out.println("Ignoring trackRecord call: this is a test!");
          }
        };
        delegate.onSessionCreated(session);
      }
    };
  }

  private long updatePassword(String password, PasswordRecord record, long timestamp) {
    record.encryptedPassword = password;
    long modifiedTime = System.currentTimeMillis();
    record.timePasswordChanged = record.lastModified = modifiedTime;
    return modifiedTime;
  }

  private long updatePassword(String password, PasswordRecord record) {
    return updatePassword(password, record, System.currentTimeMillis());
  }

  // Runnable Helpers.
  private Runnable storeRunnable(final RepositorySession session, final Record record) {
    return new Runnable() {
      @Override
      public void run() {
        session.setStoreDelegate(new ExpectStoredDelegate(record.guid));
        try {
          session.store(record);
          session.storeDone();
        } catch (NoStoreDelegateException e) {
          fail("NoStoreDelegateException should not occur.");
        }
      }
    };
  }

  public static Runnable fetchAllRunnable(final RepositorySession session, final Record[] records) {
    return new Runnable() {
      @Override
      public void run() {
        session.fetchAll(new ExpectFetchDelegate(records));
      }
    };
  }

  public Runnable guidsSinceRunnable(final RepositorySession session, final long timestamp, final String[] expected) {
    return new Runnable() {
      @Override
      public void run() {
        session.guidsSince(timestamp, new ExpectGuidsSinceDelegate(expected));
      }
    };
  }

  public Runnable fetchSinceRunnable(final RepositorySession session, final long timestamp, final String[] expected) {
    return new Runnable() {
      @Override
      public void run() {
        session.fetchSince(timestamp, new ExpectFetchSinceDelegate(timestamp, expected));
      }
    };
  }

  public Runnable fetchRunnable(final RepositorySession session, final String[] guids, final Record[] expected) {
    return new Runnable() {
      @Override
      public void run() {
        try {
          session.fetch(guids, new ExpectFetchDelegate(expected));
        } catch (InactiveSessionException e) {
          performNotify(e);
        }
      }
    };
  }
}
