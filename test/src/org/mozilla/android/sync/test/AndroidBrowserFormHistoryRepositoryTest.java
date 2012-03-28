/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.test.helpers.FormHistoryHelpers;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserDeletedFormHistoryDataAccessor;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserFormHistoryDataAccessor;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserFormHistoryRepositorySession;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserHistoryRepository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepositoryDataAccessor;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionCreationDelegate;
import org.mozilla.gecko.sync.repositories.domain.FormHistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

public class AndroidBrowserFormHistoryRepositoryTest extends AndroidBrowserRepositoryTest {

  public static boolean shouldSkip = false;

  @Override
  protected AndroidBrowserRepository getRepository() {

    /**
     * Override this chain in order to avoid our test code having to create two
     * sessions all the time.
     */
    return new AndroidBrowserHistoryRepository() {
      @Override
      protected void sessionCreator(RepositorySessionCreationDelegate delegate, Context context) {
        AndroidBrowserFormHistoryRepositorySession session;
        session = new AndroidBrowserFormHistoryRepositorySession(this, context) {
          @Override
          protected synchronized void trackRecord(Record record) {
            System.out.println("Ignoring trackRecord call: this is a test!");
          }
        };
        delegate.onSessionCreated(session);
      }
    };
  }

  protected void wipe() {
    Log.i(LOG_TAG, "Wiping...");
    getDataAccessor().wipe();
    getDeletedAccessor().wipe();
    Log.i(LOG_TAG, "Wiping... DONE");
  }

  @Override
  protected AndroidBrowserRepositoryDataAccessor getDataAccessor() {
    return new AndroidBrowserFormHistoryDataAccessor(getApplicationContext());
  }

  protected AndroidBrowserRepositoryDataAccessor getDeletedAccessor() {
    return new AndroidBrowserDeletedFormHistoryDataAccessor(getApplicationContext());
  }

  @Override
  public void testFetchAll() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    Record[] expected = new Record[2];
    expected[0] = FormHistoryHelpers.createFormHistory1();
    expected[1] = FormHistoryHelpers.createFormHistory2();
    basicFetchAllTest(expected);
  }

  public Record[] insertRegularAndDeletedRecords() {
    Record[] expected = new Record[2];
    expected[0] = FormHistoryHelpers.createFormHistory1();
    expected[1] = FormHistoryHelpers.createDeletedFormHistory2();
    getDataAccessor().insert(expected[0]);
    getDeletedAccessor().insert(expected[1]);
    return expected;
  }

  /**
   * Ensure that fetchAll returns deleted records.
   */
  public void testWithDeletedFetchAll() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    Record[] expected = insertRegularAndDeletedRecords();

    AndroidBrowserRepositorySession session = (AndroidBrowserRepositorySession)createAndBeginSession();
    performWait(fetchAllRunnable(session, expected));
  }

  @Override
  public void testGuidsSinceReturnMultipleRecords() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    FormHistoryRecord record0 = FormHistoryHelpers.createFormHistory3();
    FormHistoryRecord record1 = FormHistoryHelpers.createFormHistory2();
    guidsSinceReturnMultipleRecords(record0, record1);
  }

  public void testWithDeletedGuidsSinceReturnMultipleRecords() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    long timestamp = System.currentTimeMillis();

    Record[] expected = insertRegularAndDeletedRecords();

    AndroidBrowserRepositorySession session = (AndroidBrowserRepositorySession)createAndBeginSession();
    String [] expectedGuids = new String[] { expected[0].guid, expected[1].guid };
    performWait(guidsSinceRunnable(session, timestamp, expectedGuids));
  }

  @Override
  public void testGuidsSinceReturnNoRecords() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    guidsSinceReturnNoRecords(FormHistoryHelpers.createFormHistory1());
  }

  @Override
  public void testFetchSinceOneRecord() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    fetchSinceOneRecord(FormHistoryHelpers.createFormHistory1(),
                        FormHistoryHelpers.createFormHistory2());
  }

  /**
   * Ensure fetch since returns deleted records.
   */
  public void testWithDeletedFetchSinceOneRecord() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    RepositorySession session = createAndBeginSession();

    long after0 = System.currentTimeMillis();
    Record[] expected = insertRegularAndDeletedRecords();

    String[] expectedBoth = new String[] { expected[0].guid, expected[1].guid };
    performWait(fetchSinceRunnable(session, after0, expectedBoth));

    dispose(session);
  }

  @Override
  public void testFetchSinceReturnNoRecords() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    fetchSinceReturnNoRecords(FormHistoryHelpers.createFormHistory2());
  }

  /**
   * Ensure fetchSince filters deleted records.
   */
  public void testWithDeletedFetchSinceReturnNoRecords() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    insertRegularAndDeletedRecords();

    RepositorySession session = createAndBeginSession();
    long timestamp = System.currentTimeMillis();

    performWait(fetchSinceRunnable(session, timestamp + 2000, new String[] {}));
    dispose(session);
  }

  @Override
  public void testFetchOneRecordByGuid() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    fetchOneRecordByGuid(FormHistoryHelpers.createFormHistory1(),
                         FormHistoryHelpers.createFormHistory2());
  }


  @Override
  public void testFetchMultipleRecordsByGuids() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    FormHistoryRecord record0 = FormHistoryHelpers.createFormHistory1();
    FormHistoryRecord record1 = FormHistoryHelpers.createFormHistory2();
    FormHistoryRecord record2 = FormHistoryHelpers.createFormHistory3();
    fetchMultipleRecordsByGuids(record0, record1, record2);
  }

  public void testWithDeletedFetchMultipleRecordsByGuids() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    Record[] expected = insertRegularAndDeletedRecords();
    String[] expectedGuids = new String[] { expected[0].guid, expected[1].guid };

    RepositorySession session = createAndBeginSession();
    performWait(fetchRunnable(session, expectedGuids, expected));
    dispose(session);
  }

  @Override
  public void testFetchNoRecordByGuid() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    fetchNoRecordByGuid(FormHistoryHelpers.createFormHistory1());
  }

  @Override
  public void testWipe() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    doWipe(FormHistoryHelpers.createFormHistory1(), FormHistoryHelpers.createFormHistory2());
  }

  /**
   * Ensure that wipe kills deleted records.
   */
  public void testWithDeletedWipe() throws NullCursorException {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }

    doWipe(FormHistoryHelpers.createFormHistory1(), FormHistoryHelpers.createFormHistory2());

    Cursor cur = null;
    try {
      cur = getDataAccessor().fetchAll();
      assertEquals(0, cur.getCount());
    } finally {
      if (cur != null) {
        cur.close();
      }
    }
    try {
      cur = getDeletedAccessor().fetchAll();
      assertEquals(0, cur.getCount());
    } finally {
      if (cur != null) {
        cur.close();
      }
    }
  }

  @Override
  public void testStore() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    AndroidBrowserRepositorySession session = (AndroidBrowserRepositorySession)createAndBeginSession();
    performWait(storeRunnable(session, FormHistoryHelpers.createFormHistory1()));
  }

  @Override
  public void testRemoteNewerTimeStamp() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    FormHistoryRecord local = FormHistoryHelpers.createFormHistory2();
    FormHistoryRecord remote = FormHistoryHelpers.createFormHistory1();
    remoteNewerTimeStamp(local, remote);
  }

  @Override
  public void testLocalNewerTimeStamp() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    FormHistoryRecord local = FormHistoryHelpers.createFormHistory1();
    FormHistoryRecord remote = FormHistoryHelpers.createFormHistory2();
    localNewerTimeStamp(local, remote);
  }

  @Override
  public void testDeleteRemoteNewer() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    FormHistoryRecord local = FormHistoryHelpers.createFormHistory1();
    FormHistoryRecord remote = FormHistoryHelpers.createFormHistory2();
    deleteRemoteNewer(local, remote);
  }

  @Override
  public void testDeleteLocalNewer() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    FormHistoryRecord local = FormHistoryHelpers.createFormHistory1();
    FormHistoryRecord remote = FormHistoryHelpers.createFormHistory3();
    deleteLocalNewer(local, remote);
  }

  @Override
  public void testDeleteRemoteLocalNonexistent() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    deleteRemoteLocalNonexistent(FormHistoryHelpers.createFormHistory5());
  }

  @Override
  public void testStoreIdenticalExceptGuid() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    storeIdenticalExceptGuid(FormHistoryHelpers.createFormHistory1());
  }

  @Override
  public void testCleanMultipleRecords() {
    if (shouldSkip) {
      assertTrue(true);
      return;
    }
    cleanMultipleRecords(
        FormHistoryHelpers.createFormHistory1(),
        FormHistoryHelpers.createFormHistory2(),
        FormHistoryHelpers.createFormHistory3(),
        FormHistoryHelpers.createFormHistory4(),
        FormHistoryHelpers.createFormHistory5()
    );
  }
}
