/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.test.helpers.DefaultCleanDelegate;
import org.mozilla.android.sync.test.helpers.DefaultFetchDelegate;
import org.mozilla.android.sync.test.helpers.DefaultSessionCreationDelegate;
import org.mozilla.android.sync.test.helpers.DefaultStoreDelegate;
import org.mozilla.android.sync.test.helpers.ExpectBeginDelegate;
import org.mozilla.android.sync.test.helpers.ExpectBeginFailDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFetchDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFetchSinceDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFinishDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFinishFailDelegate;
import org.mozilla.android.sync.test.helpers.ExpectGuidsSinceDelegate;
import org.mozilla.android.sync.test.helpers.ExpectInvalidRequestFetchDelegate;
import org.mozilla.android.sync.test.helpers.ExpectStoredDelegate;
import org.mozilla.gecko.sync.StubActivity;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.InactiveSessionException;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepositoryDataAccessor;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepositorySession;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

public abstract class AndroidBrowserRepositoryTest extends ActivityInstrumentationTestCase2<StubActivity> {
  
  protected AndroidBrowserRepositoryDataAccessor helper;
  protected static final String tag = "AndroidBrowserRepositoryTest";
  
  public AndroidBrowserRepositoryTest() {
    super(StubActivity.class);
  }

  public Context getApplicationContext() {
    return this.getInstrumentation().getTargetContext().getApplicationContext();
  }

  protected void performWait(Runnable runnable) throws AssertionError {
    AndroidBrowserRepositoryTestHelper.testWaiter.performWait(runnable);
  }
  
  protected void performNotify() {
    AndroidBrowserRepositoryTestHelper.testWaiter.performNotify();
  }

  protected AndroidBrowserRepositorySession getSession() {
    return AndroidBrowserRepositoryTestHelper.session;
  }
  
  private void wipe() {
    if (helper == null) {
      helper = getDataAccessor();
    }
    try {
      helper.wipe();
    } catch (NullPointerException e) {
      // This will be handled in begin, here we can just ignore
      // the error if it actually occurs since this is just test
      // code. We will throw a ProfileDatabaseException. This
      // error shouldn't occur in the future, but results from
      // trying to access content providers before Fennec has
      // been run at least once.
      Log.e(tag, "ProfileDatabaseException seen in wipe, begin shoud fail");
      
    }
  }

  public void setUp() {
    Log.i("rnewman", "Wiping.");
    wipe();
  }
  
  protected void prepSession() {
    AndroidBrowserRepositoryTestHelper.prepareRepositorySession(getApplicationContext(),
        new SetupDelegate(), true, getRepository());
    // Clear old data.
    //wipe();
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
  
  protected Runnable getFetchAllRunnable(final ExpectFetchDelegate delegate) {
    return new Runnable() {
      public void run() {
        getSession().fetchAll(delegate);
      }
    };
  }
  
  public static Runnable storeRunnable(final RepositorySession session, final Record record, final DefaultStoreDelegate delegate) {
    return new Runnable() {
      @Override
      public void run() {
        session.store(record, delegate);
      }
    };
  }
  
  public static Runnable storeRunnable(final RepositorySession session, final Record record) {
    return storeRunnable(session, record, new ExpectStoredDelegate(record.guid));
  }

  public static Runnable fetchAllRunnable(final RepositorySession session, final ExpectFetchDelegate delegate) {
    return new Runnable() {
      @Override
      public void run() {
        session.fetchAll(delegate);        
      }
    };
  }
  public static Runnable fetchAllRunnable(final RepositorySession session, final Record[] expectedRecords) {
    return fetchAllRunnable(session, new ExpectFetchDelegate(expectedRecords));
  }
  
  public static Runnable guidsSinceRunnable(final RepositorySession session, final long timestamp, final String[] expected) {
    return new Runnable() {
      @Override
      public void run() {
        session.guidsSince(timestamp, new ExpectGuidsSinceDelegate(expected));
      }
    };
  }  
  public static Runnable fetchSinceRunnable(final RepositorySession session, final long timestamp, final String[] expected) {
    return new Runnable() {
      @Override
      public void run() {
        session.fetchSince(timestamp, new ExpectFetchSinceDelegate(timestamp, expected));
      }
    };
  }
  
  public static Runnable fetchRunnable(final RepositorySession session, final String[] guids, final DefaultFetchDelegate delegate) {
    return new Runnable() {
      @Override
      public void run() {
        session.fetch(guids, delegate);
      }
    };    
  }
  public static Runnable fetchRunnable(final RepositorySession session, final String[] guids, final Record[] expected) {
    return fetchRunnable(session, guids, new ExpectFetchDelegate(expected));
  }
  
  public static Runnable cleanRunnable(final Repository repository, final boolean success, final Context context, final DefaultCleanDelegate delegate) {
    return new Runnable() {
      @Override
      public void run() {
        repository.clean(success, delegate, context);
        
      }
    };
  }

  protected abstract AndroidBrowserRepository getRepository();
  protected abstract AndroidBrowserRepositoryDataAccessor getDataAccessor();
  
  protected void doStore(RepositorySession session, Record[] records) {
    for (int i = 0; i < records.length; i++) {
      performWait(storeRunnable(session, records[i]));
    }
  }
  
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
  public abstract void testDeleteLocalNewer();
  public abstract void testDeleteRemoteLocalNonexistent();
  public abstract void testStoreIdenticalExceptGuid();
  public abstract void testCleanMultipleRecords();
  public abstract void testCleanSuccessFalse();
  
  /*
   * Test abstractions
   */
  protected void basicStoreTest(Record record) {
    prepSession();    
    performWait(storeRunnable(getSession(), record));
  }
  
  protected void basicFetchAllTest(Record[] expected) {
    Log.i("rnewman", "Starting testFetchAll.");
    AndroidBrowserRepositoryTestHelper.prepareRepositorySession(getApplicationContext(),
        new SetupDelegate(), true, getRepository());
    Log.i("rnewman", "Prepared.");

    AndroidBrowserRepositorySession session = getSession();
    for (Record record : expected) {
      performWait(storeRunnable(session, record)); 
    }
    performWait(fetchAllRunnable(session, expected));
  }
  
  /*
   * Tests for clean
   */
  // Input: 4 records; 2 which are to be cleaned, 2 which should remain after the clean
  protected void cleanMultipleRecords(Record delete0, Record delete1, Record keep0, Record keep1) {
    prepSession();
    AndroidBrowserRepositorySession session = getSession();
    delete0.deleted = true;
    delete1.deleted = true;
    doStore(session, new Record[] {
        delete0, delete1, keep0, keep1
    });
    performWait(cleanRunnable(getRepository(), true, getApplicationContext(), new DefaultCleanDelegate() {
      public void onCleaned(Repository repo) {
        testWaiter().performNotify();
      }
    })); 
    
    performWait(fetchAllRunnable(session, new ExpectFetchDelegate(new Record[] { keep0, keep1})));
  }
  
  // Store 2 records, 1 marked deleted. Call clean with success = false, both records should remain.
  protected void cleanSuccessFalse(Record record0, Record record1) {
    prepSession();
    AndroidBrowserRepositorySession session = getSession();
    record0.deleted = true;
    doStore(session, new Record[] {
        record0, record1
    });
    performWait(cleanRunnable(getRepository(), false, getApplicationContext(), new DefaultCleanDelegate() {
      public void onCleaned(Repository repo) {
        testWaiter().performNotify();
      }
    })); 
    
    performWait(fetchAllRunnable(session, new ExpectFetchDelegate(new Record[] { record0, record1})));
  }
  
  /*
   * Tests for guidsSince
   */
  protected void guidsSinceReturnMultipleRecords(Record record0, Record record1) {
    prepEmptySession();
    AndroidBrowserRepositorySession session = getSession();
    long timestamp = System.currentTimeMillis();

    String[] expected = new String[2];
    expected[0] = record0.guid;
    expected[1] = record1.guid;
    record0.lastModified = timestamp + 1000;
    record1.lastModified = timestamp + 1500;

    performWait(storeRunnable(session, record0));
    performWait(storeRunnable(session, record1));
    performWait(guidsSinceRunnable(session, timestamp, expected));
  }
  
  protected void guidsSinceReturnNoRecords(Record record0) {
    prepEmptySession();
    AndroidBrowserRepositorySession session = getSession();

    //  Store 1 record in the past.
    performWait(storeRunnable(session, record0));

    String[] expected = {};
    performWait(guidsSinceRunnable(session, System.currentTimeMillis() + 1000, expected));
  }

  /*
   * Tests for fetchSince
   */  
  protected void fetchSinceOneRecord(Record record0, Record record1) {
    prepSession();
    AndroidBrowserRepositorySession session = getSession();

    performWait(storeRunnable(session, record0));
    long timestamp = System.currentTimeMillis();
    synchronized(this) {
    try {
      wait(1000);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    }
    performWait(storeRunnable(session, record1));

    // Fetch just record1 
    String[] expectedOne = new String[1];
    expectedOne[0] = record1.guid;
    performWait(fetchSinceRunnable(session, timestamp + 10, expectedOne));

    // Fetch both, relying on inclusiveness.
    String[] expectedBoth = new String[2];
    expectedBoth[0] = record0.guid;
    expectedBoth[1] = record1.guid;
    performWait(fetchSinceRunnable(session, timestamp - 3000, expectedBoth));
  }
  
  protected void fetchSinceReturnNoRecords(Record record) {
    prepEmptySession();
    AndroidBrowserRepositorySession session = getSession();
    
    performWait(storeRunnable(session, record));

    long timestamp = System.currentTimeMillis();

    performWait(fetchSinceRunnable(session, timestamp + 2000, new String[] {}));
  }
  
  protected void fetchOneRecordByGuid(Record record0, Record record1) {
    prepEmptySession();
    AndroidBrowserRepositorySession session = getSession();
    
    performWait(storeRunnable(session, record0));
    performWait(storeRunnable(session, record1));
    
    performWait(fetchRunnable(session, new String[] { record0.guid }, 
        new Record[] { record0 }));
  }
  
  protected void fetchMultipleRecordsByGuids(Record record0,
      Record record1, Record record2) {
    prepEmptySession();
    AndroidBrowserRepositorySession session = getSession();

    performWait(storeRunnable(session, record0));
    performWait(storeRunnable(session, record1));
    performWait(storeRunnable(session, record2));
    
    String[] guids = new String[] { record0.guid, record2.guid };
    Record[] expected = new Record[] { record0, record2 };
    performWait(fetchRunnable(session, guids, expected));
  }
  
  protected void fetchNoRecordByGuid(Record record) {
    prepEmptySession();
    AndroidBrowserRepositorySession session = getSession();
    
    performWait(storeRunnable(session, record));
    performWait(fetchRunnable(session,
                              new String[] { Utils.generateGuid() }, 
                              new Record[] {}));
  }
  
  /*
   * Test wipe
   */
  protected void doWipe(Record record0, Record record1) {
    prepEmptySession();
    final AndroidBrowserRepositorySession session = getSession();
    
    // Store 2 records.
    performWait(storeRunnable(session, record0));
    performWait(storeRunnable(session, record1));
    performWait(fetchAllRunnable(session, 
        new Record[] { record0, record1 }));

    // Wipe
    Runnable run = new Runnable() {
      @Override
      public void run() {
        session.wipe(new RepositorySessionWipeDelegate() {
          public void onWipeSucceeded() {
            performNotify();
          }
          public void onWipeFailed(Exception ex) {
            fail("wipe should have succeeded");
            performNotify();
          }
        });
      }
    };
    performWait(run);
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
    AndroidBrowserRepositorySession session = getSession();

    // Record existing and hasn't changed since before lastSync.
    // Automatically will be assigned lastModified = current time.
    performWait(storeRunnable(session, local));

    remote.guid = local.guid;
    
    // Get the timestamp and make remote newer than it
    ExpectFetchDelegate timestampDelegate = new ExpectFetchDelegate(new Record[] { local });
    performWait(fetchRunnable(session, new String[] { remote.guid }, timestampDelegate));
    remote.lastModified = timestampDelegate.records.get(0).lastModified + 1000;
    performWait(storeRunnable(session, remote));

    Record[] expected = new Record[] { remote };
    ExpectFetchDelegate delegate = new ExpectFetchDelegate(expected);
    performWait(fetchAllRunnable(session, delegate));
  }

  /*
   * Local record has a newer timestamp than the record being stored. For now,
   * we just take newer (local) record)
   */
  protected void localNewerTimeStamp(Record local, Record remote) {
    prepSession();
    AndroidBrowserRepositorySession session = getSession();

    performWait(storeRunnable(session, local));

    remote.guid = local.guid;
    
    
    // Get the timestamp and make remote older than it
    ExpectFetchDelegate timestampDelegate = new ExpectFetchDelegate(new Record[] { local });
    performWait(fetchRunnable(session, new String[] { remote.guid }, timestampDelegate));
    remote.lastModified = timestampDelegate.records.get(0).lastModified - 1000;
    performWait(storeRunnable(session, remote));
    
    // Do a fetch and make sure that we get back the local record.
    Record[] expected = new Record[] { local };
    performWait(fetchAllRunnable(session, new ExpectFetchDelegate(expected)));
  }
  
  /*
   * Insert a record that is marked as deleted, remote has newer timestamp
   */
  protected void deleteRemoteNewer(Record local, Record remote) {
    prepSession();
    AndroidBrowserRepositorySession session = getSession();
    
    // Record existing and hasn't changed since before lastSync.
    // Automatically will be assigned lastModified = current time.
    performWait(storeRunnable(session, local));

    // Pass the same record to store, but mark it deleted and modified
    // more recently
    ExpectFetchDelegate timestampDelegate = new ExpectFetchDelegate(new Record[] { local });
    performWait(fetchRunnable(session, new String[] { local.guid }, timestampDelegate));
    remote.lastModified = timestampDelegate.records.get(0).lastModified + 1000;
    remote.deleted = true;
    remote.guid = local.guid;
    performWait(storeRunnable(session, remote));

    performWait(fetchAllRunnable(session, new ExpectFetchDelegate(new Record[]{})));
  }
  
  // Store two records that are identical (this has different meanings based on the
  // type of record) other than their guids. The record existing locally already
  // should have its guid replaced (the assumption is that the record existed locally
  // and then sync was enabled and this record existed on another sync'd device).
  public void storeIdenticalExceptGuid(Record record0, Record record1) {
    prepSession();
    AndroidBrowserRepositorySession session = getSession();
    
    performWait(storeRunnable(session, record0));
    
    ExpectFetchDelegate timestampDelegate = new ExpectFetchDelegate(new Record[] { record0 });
    performWait(fetchRunnable(session, new String[] { record0.guid }, timestampDelegate));
    record1.lastModified = timestampDelegate.records.get(0).lastModified + 3000;
    performWait(storeRunnable(session, record1));
    
    performWait(fetchAllRunnable(session, new ExpectFetchDelegate(
        new Record[] { record1 })));
  }
  
  /*
   * Insert a record that is marked as deleted, local has newer timestamp
   * and was not marked deleted (so keep it)
   */
  protected void deleteLocalNewer(Record local, Record remote) {
    prepSession();
    AndroidBrowserRepositorySession session = getSession();

    performWait(storeRunnable(session, local));

    // Create an older version of a record with the same GUID.
    remote.guid = local.guid;
    
    // Get the timestamp and make remote older than it
    ExpectFetchDelegate timestampDelegate = new ExpectFetchDelegate(new Record[] { local });
    performWait(fetchRunnable(session, new String[] { remote.guid }, timestampDelegate));
    remote.lastModified = timestampDelegate.records.get(0).lastModified - 1000;
    remote.deleted = true;
    performWait(storeRunnable(session, remote));

    // Do a fetch and make sure that we get back the first (local) record.
    Record[] expected = new Record[] { local };
    performWait(fetchAllRunnable(session, new ExpectFetchDelegate(expected)));
  }
  
  /*
   * Insert a record that is marked as deleted, record never existed locally
   */
  protected void deleteRemoteLocalNonexistent(Record remote) {
    prepSession();
    AndroidBrowserRepositorySession session = getSession();
    
    long timestamp = 1000000000;
    
    // Pass a record marked deleted to store, doesn't exist locally
    remote.lastModified = timestamp;
    remote.deleted = true;
    performWait(storeRunnable(session, remote));

    ExpectFetchDelegate delegate = new ExpectFetchDelegate(new Record[]{});
    performWait(fetchAllRunnable(session, delegate));
  }
  
  /*
   * Tests that don't require specific records based on type of repository.
   * These tests don't need to be overriden in subclasses, they will just work.
   */
  public void testCreateSessionNullContext() {
    Log.i("rnewman", "In testCreateSessionNullContext.");
    AndroidBrowserRepository repo = getRepository();
    try {
      repo.createSession(new DefaultSessionCreationDelegate(), null);
      fail("Should throw.");
    } catch (Exception ex) {
      assertNotNull(ex);
    }
  }
  
  public void testStoreNullRecord() {
    prepSession();
    try {
      AndroidBrowserRepositorySession session = getSession();
      session.store(null, new DefaultStoreDelegate());
      fail("Should throw.");
    } catch (Exception ex) {
      assertNotNull(ex);
    }
  }
  
  public void testFetchNoGuids() {
    prepEmptySession();
    AndroidBrowserRepositorySession session = getSession();
    performWait(fetchRunnable(session, new String[] {}, new ExpectInvalidRequestFetchDelegate())); 
  }
  
  public void testFetchNullGuids() {
    prepEmptySession();
    AndroidBrowserRepositorySession session = getSession();
    performWait(fetchRunnable(session, null, new ExpectInvalidRequestFetchDelegate())); 
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
    AndroidBrowserRepositorySession session = getSession();
    session.finish(new ExpectFinishDelegate());
    session.begin(new ExpectBeginFailDelegate());
  }
  
  public void testFinishOnFinishedSession() {
    prepEmptySession();
    AndroidBrowserRepositorySession session = getSession();
    session.finish(new ExpectFinishDelegate());
    session.finish(new ExpectFinishFailDelegate());
  }
  
  public void testFetchOnInactiveSession() {
    prepEmptySessionWithoutBegin();
    getSession().finish(new ExpectFinishFailDelegate());
  }
  
  public void testFetchOnFinishedSession() {
    prepEmptySession();
    Runnable run = new Runnable() {
      @Override
      public void run() {
        getSession().finish(new ExpectFinishDelegate());
        getSession().fetch(new String[] { Utils.generateGuid() },
            new RepositorySessionFetchRecordsDelegate() {
              @Override
              public void onFetchSucceeded(Record[] records, long end) {
                fail("Session inactive, should fail");
                performNotify();
              }

              @Override
              public void onFetchFailed(Exception ex, Record record) {
                verifyInactiveException(ex);
                performNotify();
              }

              @Override
              public void onFetchedRecord(Record record) {
                fail("Session inactive, should fail");
                performNotify();
              }

              @Override
              public void onFetchCompleted(long end) {
                fail("Session inactive, should fail");
                performNotify();
              }
            });
      }
    };
    performWait(run);
  }
  
  public void testGuidsSinceOnUnstartedSession() {
    prepEmptySessionWithoutBegin();
    Runnable run = new Runnable() {
      @Override
      public void run() {
        getSession().guidsSince(System.currentTimeMillis(),
            new RepositorySessionGuidsSinceDelegate() {
              public void onGuidsSinceSucceeded(String[] guids) {
                fail("Session inactive, should fail");
                performNotify();
              }

              public void onGuidsSinceFailed(Exception ex) {
                verifyInactiveException(ex);
                performNotify();
              }
            });
      }
    };
    performWait(run);
  }
  
  private void verifyInactiveException(Exception ex) {
    if (ex.getClass() != InactiveSessionException.class) {
      fail("Wrong exception type");
    }
  }
}
