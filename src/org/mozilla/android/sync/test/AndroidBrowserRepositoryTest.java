/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.MainActivity;
import org.mozilla.android.sync.repositories.InactiveSessionException;
import org.mozilla.android.sync.repositories.Utils;
import org.mozilla.android.sync.repositories.android.AndroidBrowserRepository;
import org.mozilla.android.sync.repositories.android.AndroidBrowserRepositoryDatabaseHelper;
import org.mozilla.android.sync.repositories.android.AndroidBrowserRepositorySession;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.android.sync.repositories.domain.Record;
import org.mozilla.android.sync.test.helpers.DefaultSessionCreationDelegate;
import org.mozilla.android.sync.test.helpers.DefaultStoreDelegate;
import org.mozilla.android.sync.test.helpers.ExpectBeginDelegate;
import org.mozilla.android.sync.test.helpers.ExpectBeginFailDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFetchAllDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFetchDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFetchSinceDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFinishDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFinishFailDelegate;
import org.mozilla.android.sync.test.helpers.ExpectGuidsSinceDelegate;
import org.mozilla.android.sync.test.helpers.ExpectInvalidRequestFetchDelegate;
import org.mozilla.android.sync.test.helpers.ExpectStoredDelegate;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

public abstract class AndroidBrowserRepositoryTest extends ActivityInstrumentationTestCase2<MainActivity> {
  
  protected AndroidBrowserRepositoryDatabaseHelper helper;
  
  public AndroidBrowserRepositoryTest() {
    super(MainActivity.class);
  }

  public Context getApplicationContext() {
    return this.getInstrumentation().getTargetContext().getApplicationContext();
  }

  protected void performWait(Runnable runnable) throws AssertionError {
    AndroidBrowserRepositoryTestHelper.testWaiter.performWait(runnable);
  }
  
  // TODO move away from these and use only runnables
  protected void performWait() {
    AndroidBrowserRepositoryTestHelper.testWaiter.performWait();
  }
  protected void performNotfiy() {
    AndroidBrowserRepositoryTestHelper.testWaiter.performNotify();
  }

  protected AndroidBrowserRepositorySession getSession() {
    return AndroidBrowserRepositoryTestHelper.session;
  }
  
  public void tearDown() {
    if (helper != null) {
      helper.close();
    }
  }
  
  private void wipe() {
    if (helper == null) {
      helper = getDatabaseHelper();
    }
    helper.wipe();
  }

  public void setUp() {
    Log.i("rnewman", "Wiping.");
    wipe();
  }
  
  protected void prepSession() {
    AndroidBrowserRepositoryTestHelper.prepareRepositorySession(getApplicationContext(),
        new SetupDelegate(), 0, true, getRepository());
    // Clear old data.
    wipe();
  }
  
  protected void prepEmptySession() {
    AndroidBrowserRepositoryTestHelper.prepEmptySession(getApplicationContext(), getRepository());
  }
  
  protected void prepEmptySessionWithoutBegin() {
    AndroidBrowserRepositoryTestHelper.prepEmptySessionWithoutBegin(getApplicationContext(), getRepository());
  }
  
  protected Runnable getStoreRunnable(final Record record, final ExpectStoredDelegate delegate) {
    return new Runnable() {
      public void run() {
        getSession().store(record, delegate);
      }
    };
  }
  
  protected Runnable getFetchAllRunnable(final ExpectFetchAllDelegate delegate) {
    return new Runnable() {
      public void run() {
        getSession().fetchAll(delegate);
      }
    };
  }
  
  protected abstract AndroidBrowserRepository getRepository();
  protected abstract AndroidBrowserRepositoryDatabaseHelper getDatabaseHelper();
  protected abstract void verifyExpectedRecordReturned(Record expected, Record actual);
  
  // Tests to implement
  public abstract void testFetchAll();
  public abstract void testGuidsSinceReturnMultipleRecords();
  public abstract void testGuidsSinceReturnNoRecords();
  public abstract void testFetchSinceOneRecord();
  public abstract void testFetchSinceReturnNoRecords();
  public abstract void testFetchOneRecordByGuid();
  public abstract void testFetchMultipleRecordsByGuids();
  public abstract void testFetchNoRecordByGuid();
  public abstract void testWipe();
  public abstract void testStore();
  public abstract void testRemoteNewerTimeStamp();
  public abstract void testLocalNewerTimeStamp();
  public abstract void testDeleteRemoteNewer();
  public abstract void testDeleteLocalNewere();
  public abstract void testDeleteRemoteLocalNonexistent();
  
  /*
   * Test abstractions
   */
  protected void basicStoreTest(Record record) {
    prepSession();
    Runnable runnable = getStoreRunnable(record, new ExpectStoredDelegate(record.guid));
    performWait(runnable);
  }
  
  protected void basicFetchAllTest(Record[] expected) {
    Log.i("rnewman", "Starting testFetchAll.");
    AndroidBrowserRepositoryTestHelper.prepareRepositorySession(getApplicationContext(),
        new SetupDelegate(), 0, true, getRepository());
    Log.i("rnewman", "Prepared.");
    String[] expectedGUIDs = new String[expected.length];
    for (int i = 0; i < expectedGUIDs.length; i++) {
      expectedGUIDs[i] = expected[i].guid;
    }

    AndroidBrowserRepositorySession session = getSession();
    session.store(expected[0], new ExpectStoredDelegate(expected[0].guid));
    performWait();
    session.store(expected[1], new ExpectStoredDelegate(expected[1].guid));
    performWait();   
    
    ExpectFetchAllDelegate delegate = new ExpectFetchAllDelegate(expectedGUIDs);
    session.fetchAll(delegate);
    performWait();

    assertEquals(delegate.recordCount(), expected.length);
  }
  
  /*
   * Tests for guidsSince
   */
  protected void guidsSinceReturnMultipleRecords(Record record0, Record record1) {
    prepEmptySession();
    long timestamp = System.currentTimeMillis();

    String[] expected = new String[2];
    expected[0] = record0.guid;
    expected[1] = record1.guid;
    record0.lastModified = timestamp + 1000;
    record1.lastModified = timestamp + 1500;

    getSession().store(record0, new ExpectStoredDelegate(expected[0]));
    performWait();
    getSession().store(record1, new ExpectStoredDelegate(expected[1]));
    performWait();
    
    getSession().guidsSince(timestamp, new ExpectGuidsSinceDelegate(expected));
    performWait();
  }
  
  protected void guidsSinceReturnNoRecords(Record record0) {
    prepEmptySession();
    long timestamp = System.currentTimeMillis();

    //  Store 1 record in the past.
    record0.lastModified = timestamp - 1000;
    getSession().store(record0, new ExpectStoredDelegate(record0.guid));
    performWait();

    String[] expected = {};
    getSession().guidsSince(timestamp, new ExpectGuidsSinceDelegate(expected));
    performWait();
  }

  /*
   * Tests for fetchSince
   */  
  protected void fetchSinceOneRecord(Record record0, Record record1) {
    prepEmptySession();
    long timestamp = System.currentTimeMillis();

    record0.lastModified = timestamp;       // Verify inclusive retrieval.
    getSession().store(record0, new ExpectStoredDelegate(record0.guid));
    performWait();

    record1.lastModified = timestamp + 3000;
    getSession().store(record1, new ExpectStoredDelegate(record1.guid));
    performWait();

    // Fetch just record1 
    String[] expectedOne = new String[1];
    expectedOne[0] = record1.guid;
    getSession().fetchSince(timestamp + 1, new ExpectFetchSinceDelegate(timestamp, expectedOne));
    performWait();

    // Fetch both, relying on inclusiveness.
    String[] expectedBoth = new String[2];
    expectedBoth[0] = record0.guid;
    expectedBoth[1] = record1.guid;
    getSession().fetchSince(timestamp, new ExpectFetchSinceDelegate(timestamp, expectedBoth));
    performWait();
  }
  
  protected void fetchSinceReturnNoRecords(Record record) {
    prepEmptySession();
    
    getSession().store(record, new ExpectStoredDelegate(record.guid));
    performWait();

    long timestamp = System.currentTimeMillis()/1000;

    getSession().fetchSince(timestamp, new ExpectFetchSinceDelegate(timestamp, new String[] { }));
    performWait();
  }
  
  protected void fetchOneRecordByGuid(Record record0, Record record1) {
    prepEmptySession();
    
    getSession().store(record0, new ExpectStoredDelegate(record0.guid));
    performWait();
    getSession().store(record1, new ExpectStoredDelegate(record1.guid));
    performWait();
    
    String[] expected = new String[] { record0.guid };
    getSession().fetch(expected, new ExpectFetchDelegate(expected));
    performWait();
  }
  
  protected void fetchMultipleRecordsByGuids(Record record0,
      Record record1, Record record2) {
    prepEmptySession();

    getSession().store(record0, new ExpectStoredDelegate(record0.guid));
    performWait();
    getSession().store(record1, new ExpectStoredDelegate(record1.guid));
    performWait();
    getSession().store(record2, new ExpectStoredDelegate(record2.guid));
    performWait();
    
    String[] expected = new String[] { record0.guid, record2.guid };
    getSession().fetch(expected, new ExpectFetchDelegate(expected));
    performWait();
  }
  
  protected void fetchNoRecordByGuid(Record record) {
    prepEmptySession();
    
    getSession().store(record, new ExpectStoredDelegate(record.guid));
    performWait();
    
    getSession().fetch(new String[] { Utils.generateGuid() }, 
        new ExpectFetchDelegate(new String[]{}));
    performWait();
  }
  
  /*
   * Test wipe
   */
  protected void doWipe(Record record0, Record record1) {
    prepEmptySession();
    
    // Store 2 records
    getSession().store(record0, new ExpectStoredDelegate(record0.guid));
    performWait();
    getSession().store(record1, new ExpectStoredDelegate(record1.guid));
    performWait();
    getSession().fetchAll(new ExpectFetchAllDelegate(new String[] {
        record0.guid, record1.guid
    }));
    performWait();
    
    // Wipe
    getSession().wipe(new RepositorySessionWipeDelegate() {
      public void onWipeSucceeded() {
        performNotfiy();
      }
      public void onWipeFailed(Exception ex) {
        fail("wipe should have succeeded");
        performNotfiy();
      }
    });
    performWait();
  }
  
  /*
   * Test for store conflict resolution
   * NOTE: Must set an android ID for local record for these tests to work
   */

  /*
   * Record being stored has newer timestamp than existing local record, local
   * record has not been modified since last sync.
   */
  protected void remoteNewerTimeStamp(Record local, Record remote) {
    prepSession();

    // Record existing and hasn't changed since before lastSync.
    // Automatically will be assigned lastModified = current time.
    Runnable localRunnable = getStoreRunnable(local, new ExpectStoredDelegate(local.guid));
    performWait(localRunnable);

    // Create second bookmark to be passed to store. Give it a later
    // last modified timestamp and set it as same GUID.
    remote.guid = local.guid;
    remote.lastModified = local.lastModified + 1000;
    Runnable remoteRunnable = getStoreRunnable(remote, new ExpectStoredDelegate(remote.guid));
    performWait(remoteRunnable);

    String[] expected = new String[] { local.guid };
    ExpectFetchAllDelegate delegate = new ExpectFetchAllDelegate(expected);
    performWait(getFetchAllRunnable(delegate));

    // Check that one record comes back, it is the remote one, and has android
    // ID same as first.
    assertEquals(1, delegate.records.length);
    verifyExpectedRecordReturned(remote, delegate.records[0]);
  }

  /*
   * Local record has a newer timestamp than the record being stored. For now,
   * we just take newer (local) record)
   */
  protected void localNewerTimeStamp(Record local, Record remote) {
    prepSession();

    // Local record newer.
    long timestamp = 1000000000;
    local.lastModified = timestamp;
    Runnable localRunnable = getStoreRunnable(local, new ExpectStoredDelegate(local.guid));
    performWait(localRunnable);

    // Create an older version of a record with the same GUID.
    remote.guid = local.guid;
    remote.lastModified = timestamp - 100;
    Runnable remoteRunnable = getStoreRunnable(remote, new ExpectStoredDelegate(remote.guid));
    performWait(remoteRunnable);

    // Do a fetch and make sure that we get back the first (local) record.
    String[] expected = new String[] { local.guid };
    ExpectFetchAllDelegate delegate = new ExpectFetchAllDelegate(expected);
    performWait(getFetchAllRunnable(delegate));

    // Check that one record comes back, it is the local one
    assertEquals(1, delegate.recordCount());
    verifyExpectedRecordReturned(local, delegate.records[0]);
  }
  
  /*
   * Insert a record that is marked as deleted, remote has newer timestamp
   */
  
  protected void deleteRemoteNewer(Record local, Record remote) {
    prepSession();
    
    // Record existing and hasn't changed since before lastSync.
    // Automatically will be assigned lastModified = current time.
    Runnable local1Runnable = getStoreRunnable(local, new ExpectStoredDelegate(local.guid));
    performWait(local1Runnable);

    // Pass the same record to store, but mark it deleted and modified
    // more recently
    local.lastModified = local.lastModified + 1000;
    local.deleted = true;
    Runnable local2Runnable = getStoreRunnable(local, new ExpectStoredDelegate(local.guid));
    performWait(local2Runnable);

    String[] expected = new String[] { local.guid };
    ExpectFetchAllDelegate delegate = new ExpectFetchAllDelegate(expected);
    performWait(getFetchAllRunnable(delegate));

    // Check that one record comes back, marked deleted and with
    // and androidId
    assertEquals(1, delegate.records.length);
    Record record = delegate.records[0];
    verifyExpectedRecordReturned(local, record);
    assertEquals(true, record.deleted);
    
  }
  
  /*
   * Insert a record that is marked as deleted, local has newer timestamp
   * and was not marked deleted (so keep it)
   */
  protected void deleteLocalNewer(Record local, Record remote) {
    prepSession();

    // Local record newer.
    long timestamp = 1000000000;
    local.lastModified = timestamp;
    Runnable localRunnable = getStoreRunnable(local, new ExpectStoredDelegate(local.guid));
    performWait(localRunnable);

    // Create an older version of a record with the same GUID.
    remote.guid = local.guid;
    remote.lastModified = timestamp - 100;
    remote.deleted = true;
    Runnable remoteRunnable = getStoreRunnable(remote, new ExpectStoredDelegate(remote.guid));
    performWait(remoteRunnable);

    // Do a fetch and make sure that we get back the first (local) record.
    String[] expected = new String[] { local.guid };
    ExpectFetchAllDelegate delegate = new ExpectFetchAllDelegate(expected);
    performWait(getFetchAllRunnable(delegate));

    // Check that one record comes back, it is the local one, and not deleted
    assertEquals(1, delegate.recordCount());
    Record record = delegate.records[0];
    verifyExpectedRecordReturned(local, record);
    assertEquals(false, record.deleted);
  }
  
  /*
   * Insert a record that is marked as deleted, record never existed locally
   */
  protected void deleteRemoteLocalNonexistent(Record remote) {
    prepSession();
    
    long timestamp = 1000000000;
    
    // Pass a record marked deleted to store, doesn't exist locally
    remote.lastModified = timestamp;
    remote.deleted = true;
    Runnable remoteRunnable = getStoreRunnable(remote, new ExpectStoredDelegate(remote.guid));
    performWait(remoteRunnable);

    ExpectFetchAllDelegate delegate = new ExpectFetchAllDelegate(new String[]{});
    performWait(getFetchAllRunnable(delegate));

    // Check that no records are returned
    assertEquals(0, delegate.records.length);
  }
  
  /*
   * Tests that don't require specific records based on type of repository.
   * These tests don't need to be overriden in subclasses, they will just work.
   */
  public void testCreateSessionNullContext() {
    Log.i("rnewman", "In testCreateSessionNullContext.");
    AndroidBrowserRepository repo = getRepository();
    try {
      repo.createSession(new DefaultSessionCreationDelegate(), null, 0);
      fail("Should throw.");
    } catch (Exception ex) {
      assertNotNull(ex);
    }
  }
  
  public void testStoreNullRecord() {
    prepSession();
    try {
      getSession().store(null, new DefaultStoreDelegate());
      fail("Should throw.");
    } catch (Exception ex) {
      assertNotNull(ex);
    }
  }
  
  public void testFetchNoGuids() {
    prepEmptySession();
    getSession().fetch(new String[] {}, new ExpectInvalidRequestFetchDelegate());
    performWait();
  }
  
  public void testFetchNullGuids() {
    prepEmptySession();
    getSession().fetch(null, new ExpectInvalidRequestFetchDelegate());
    performWait();
  }
  
  public void testBeginOnNewSession() {
    prepEmptySessionWithoutBegin();
    getSession().begin(new ExpectBeginDelegate());
  }
  
  public void testBeginOnRunningSession() {
    prepEmptySession();
    getSession().begin(new ExpectBeginFailDelegate());
  }
  
  public void testBeginOnFinishedSession() {
    prepEmptySession();
    getSession().finish(new ExpectFinishDelegate());
    getSession().begin(new ExpectBeginFailDelegate());
  }
  
  public void testFinishOnFinishedSession() {
    prepEmptySession();
    getSession().finish(new ExpectFinishDelegate());
    getSession().finish(new ExpectFinishFailDelegate());
  }
  
  public void testFetchOnInactiveSession() {
    prepEmptySessionWithoutBegin();
    getSession().finish(new ExpectFinishFailDelegate());
  }
  
  public void testFetchOnFinishedSession() {
    prepEmptySession();
    getSession().finish(new ExpectFinishDelegate());
    getSession().fetch(new String[] { Utils.generateGuid() }, new RepositorySessionFetchRecordsDelegate() {
      public void onFetchSucceeded(Record[] records) {
        fail("Session inactive, should fail");
        performNotfiy();
      }
      public void onFetchFailed(Exception ex) {
        verifyInactiveException(ex);
        performNotfiy();
      }
    });
    performWait();
  }
  
  public void testGuidsSinceOnUnstartedSession() {
    prepEmptySessionWithoutBegin();
    getSession().guidsSince(System.currentTimeMillis(), new RepositorySessionGuidsSinceDelegate() {
      public void onGuidsSinceSucceeded(String[] guids) {
        fail("Session inactive, should fail");
        performNotfiy();
      }
      public void onGuidsSinceFailed(Exception ex) {
        verifyInactiveException(ex);
        performNotfiy();
      }
    });
    performWait();
  }
  
  private void verifyInactiveException(Exception ex) {
    if (ex.getClass() != InactiveSessionException.class) {
      fail("Wrong exception type");
    }
  }
  
 
  
}