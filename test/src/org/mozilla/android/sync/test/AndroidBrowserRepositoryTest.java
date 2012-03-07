/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import java.util.concurrent.ExecutorService;

import org.mozilla.android.sync.test.helpers.DefaultBeginDelegate;
import org.mozilla.android.sync.test.helpers.DefaultCleanDelegate;
import org.mozilla.android.sync.test.helpers.DefaultFetchDelegate;
import org.mozilla.android.sync.test.helpers.DefaultFinishDelegate;
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
import org.mozilla.android.sync.test.helpers.ExpectManyStoredDelegate;
import org.mozilla.android.sync.test.helpers.ExpectStoreCompletedDelegate;
import org.mozilla.android.sync.test.helpers.ExpectStoredDelegate;
import org.mozilla.android.sync.test.helpers.SessionTestHelper;
import org.mozilla.android.sync.test.helpers.WaitHelper;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.InactiveSessionException;
import org.mozilla.gecko.sync.repositories.InvalidSessionTransitionException;
import org.mozilla.gecko.sync.repositories.NoStoreDelegateException;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.RepositorySession;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepositoryDataAccessor;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.gecko.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.Context;
import android.util.Log;

public abstract class AndroidBrowserRepositoryTest extends AndroidSyncTestCase {

  protected AndroidBrowserRepositoryDataAccessor helper;

  protected static String LOG_TAG = "AndroidBrowserRepositoryTest";

  protected void wipe() {
    Log.i(LOG_TAG, "Wiping.");
    try {
      helper.wipe();
    } catch (NullPointerException e) {
      // This will be handled in begin, here we can just ignore
      // the error if it actually occurs since this is just test
      // code. We will throw a ProfileDatabaseException. This
      // error shouldn't occur in the future, but results from
      // trying to access content providers before Fennec has
      // been run at least once.
      Log.e(LOG_TAG, "ProfileDatabaseException seen in wipe. Begin should fail");
      fail("NullPointerException in wipe.");
    }
  }

  public void setUp() {
    helper = getDataAccessor();
    wipe();
    assertTrue(WaitHelper.getTestWaiter().isIdle());
  }

  public void tearDown() {
    assertTrue(WaitHelper.getTestWaiter().isIdle());
  }

  protected RepositorySession createSession() {
    return SessionTestHelper.createSession(
        getApplicationContext(),
        getRepository());
  }

  protected RepositorySession createAndBeginSession() {
    return SessionTestHelper.createAndBeginSession(
        getApplicationContext(),
        getRepository());
  }

  protected void dispose(RepositorySession session) {
    if (session == null) {
      return;
    }
    session.abort();
  }

  /**
   * Hook to return an ExpectFetchDelegate, possibly with special GUIDs ignored.
   */
  public ExpectFetchDelegate preparedExpectFetchDelegate(Record[] expected) {
    return new ExpectFetchDelegate(expected);
  }

  /**
   * Hook to return an ExpectGuidsSinceDelegate, possibly with special GUIDs ignored.
   */
  public ExpectGuidsSinceDelegate preparedExpectGuidsSinceDelegate(String[] expected) {
    return new ExpectGuidsSinceDelegate(expected);
  }

  /**
   * Hook to return an ExpectGuidsSinceDelegate expecting only special GUIDs (if there are any).
   */
  public ExpectGuidsSinceDelegate preparedExpectOnlySpecialGuidsSinceDelegate() {
    return new ExpectGuidsSinceDelegate(new String[] {});
  }

  /**
   * Hook to return an ExpectFetchSinceDelegate, possibly with special GUIDs ignored.
   */
  public ExpectFetchSinceDelegate preparedExpectFetchSinceDelegate(long timestamp, String[] expected) {
    return new ExpectFetchSinceDelegate(timestamp, expected);
  }

  public Runnable storeRunnable(final RepositorySession session, final Record record, final DefaultStoreDelegate delegate) {
    return new Runnable() {
      @Override
      public void run() {
        session.setStoreDelegate(delegate);
        try {
          session.store(record);
          session.storeDone();
        } catch (NoStoreDelegateException e) {
          performNotify("NoStoreDelegateException should not occur.", e);
        }
      }
    };
  }

  public Runnable storeRunnable(final RepositorySession session, final Record record) {
    return storeRunnable(session, record, new ExpectStoredDelegate(record.guid));
  }

  public Runnable storeManyRunnable(final RepositorySession session, final Record[] records, final DefaultStoreDelegate delegate) {
    return new Runnable() {
      @Override
      public void run() {
        session.setStoreDelegate(delegate);
        try {
          for (Record record : records) {
            session.store(record);
          }
          session.storeDone();
        } catch (NoStoreDelegateException e) {
          performNotify("NoStoreDelegateException should not occur.", e);
        }
      }
    };
  }

  public Runnable storeManyRunnable(final RepositorySession session, final Record[] records) {
    return storeManyRunnable(session, records, new ExpectManyStoredDelegate(records));
  }

  /**
   * Store a record and don't expect a store callback until we're done.
   *
   * @param session
   * @param record
   * @return
   */
  public Runnable quietStoreRunnable(final RepositorySession session, final Record record) {
    return storeRunnable(session, record, new ExpectStoreCompletedDelegate());
  }

  public static Runnable beginRunnable(final RepositorySession session, final DefaultBeginDelegate delegate) {
    return new Runnable() {
      @Override
      public void run() {
        try {
          session.begin(delegate);
        } catch (InvalidSessionTransitionException e) {
          performNotify(e);
        }
      }
    };
  }

  public Runnable finishRunnable(final RepositorySession session, final DefaultFinishDelegate delegate) {
    return new Runnable() {
      @Override
      public void run() {
        try {
          session.finish(delegate);
        } catch (InactiveSessionException e) {
          performNotify(e);
        }
      }
    };
  }

  public static Runnable fetchAllRunnable(final RepositorySession session, final ExpectFetchDelegate delegate) {
    return new Runnable() {
      @Override
      public void run() {
        session.fetchAll(delegate);        
      }
    };
  }

  public Runnable fetchAllRunnable(final RepositorySession session, final Record[] expectedRecords) {
    return fetchAllRunnable(session, preparedExpectFetchDelegate(expectedRecords));
  }

  public Runnable guidsSinceRunnable(final RepositorySession session, final long timestamp, final String[] expected) {
    return new Runnable() {
      @Override
      public void run() {
        session.guidsSince(timestamp, preparedExpectGuidsSinceDelegate(expected));
      }
    };
  }

  public Runnable fetchSinceRunnable(final RepositorySession session, final long timestamp, final String[] expected) {
    return new Runnable() {
      @Override
      public void run() {
        session.fetchSince(timestamp, preparedExpectFetchSinceDelegate(timestamp, expected));
      }
    };
  }
  
  public Runnable fetchRunnable(final RepositorySession session, final String[] guids, final DefaultFetchDelegate delegate) {
    return new Runnable() {
      @Override
      public void run() {
        try {
          session.fetch(guids, delegate);
        } catch (InactiveSessionException e) {
          performNotify(e);
        }
      }
    };    
  }
  public Runnable fetchRunnable(final RepositorySession session, final String[] guids, final Record[] expected) {
    return fetchRunnable(session, guids, preparedExpectFetchDelegate(expected));
  }
  
  public Runnable cleanRunnable(final Repository repository, final boolean success, final Context context, final DefaultCleanDelegate delegate) {
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
    performWait(storeManyRunnable(session, records));
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
  
  
  /*
   * Test abstractions
   */
  protected void basicStoreTest(Record record) {
    final RepositorySession session = createAndBeginSession();    
    performWait(storeRunnable(session, record));
  }
  
  protected void basicFetchAllTest(Record[] expected) {
    Log.i("rnewman", "Starting testFetchAll.");
    RepositorySession session = createAndBeginSession();
    Log.i("rnewman", "Prepared.");

    helper.dumpDB();
    performWait(storeManyRunnable(session, expected));

    helper.dumpDB();
    performWait(fetchAllRunnable(session, expected));
    dispose(session);
  }
  
  /*
   * Tests for clean
   */
  // Input: 4 records; 2 which are to be cleaned, 2 which should remain after the clean
  protected void cleanMultipleRecords(Record delete0, Record delete1, Record keep0, Record keep1, Record keep2) {
    RepositorySession session = createAndBeginSession();
    doStore(session, new Record[] {
        delete0, delete1, keep0, keep1, keep2
    });

    // force two record to appear deleted
    AndroidBrowserRepositoryDataAccessor db = getDataAccessor();
    db.delete(delete0);
    db.delete(delete1);

    final DefaultCleanDelegate delegate = new DefaultCleanDelegate() {
      public void onCleaned(Repository repo) {
        performNotify();
      }
    };

    final Runnable cleanRunnable = cleanRunnable(
        getRepository(),
        true,
        getApplicationContext(),
        delegate);

    Log.i(LOG_TAG, "Before cleanRunnable");
    db.dumpDB();
    performWait(cleanRunnable);
    Log.i(LOG_TAG, "After cleanRunnable");
    db.dumpDB();
    performWait(fetchAllRunnable(session, preparedExpectFetchDelegate(new Record[] { keep0, keep1, keep2})));
    dispose(session);
  }

  /*
   * Tests for guidsSince
   */
  protected void guidsSinceReturnMultipleRecords(Record record0, Record record1) {
    RepositorySession session = createAndBeginSession();
    long timestamp = System.currentTimeMillis();

    String[] expected = new String[2];
    expected[0] = record0.guid;
    expected[1] = record1.guid;

    Log.i(getName(), "Storing two records...");
    performWait(storeManyRunnable(session, new Record[] { record0, record1 }));
    Log.i(getName(), "Getting guids since " + timestamp + "; expecting " + expected.length);
    performWait(guidsSinceRunnable(session, timestamp, expected));
    dispose(session);
  }
  
  protected void guidsSinceReturnNoRecords(Record record0) {
    RepositorySession session = createAndBeginSession();

    //  Store 1 record in the past.
    performWait(storeRunnable(session, record0));

    String[] expected = {};
    performWait(guidsSinceRunnable(session, System.currentTimeMillis() + 1000, expected));
    dispose(session);
  }

  /*
   * Tests for fetchSince.
   *
   * WARNING: This will most likely fail unless you disable store tracking.  See getRepository() in subclasses.
   */
  protected void fetchSinceOneRecord(Record record0, Record record1) {
    RepositorySession session = createAndBeginSession();

    long after0 = System.currentTimeMillis();
    performWait(storeRunnable(session, record0));
    long after1 = System.currentTimeMillis();
    performWait(storeRunnable(session, record1));

    helper.dumpDB();

    Log.i("fetchSinceOneRecord", "Fetching record 1.");
    String[] expectedOne = new String[] { record1.guid };

    performWait(fetchSinceRunnable(session, after1, expectedOne));

    Log.i("fetchSinceOneRecord", "Fetching both, relying on inclusiveness.");
    String[] expectedBoth = new String[] { record0.guid, record1.guid };
    performWait(fetchSinceRunnable(session, after0, expectedBoth));

    Log.i("fetchSinceOneRecord", "Done.");
    dispose(session);
  }

  protected void fetchSinceReturnNoRecords(Record record) {
    RepositorySession session = createAndBeginSession();

    performWait(storeRunnable(session, record));

    long timestamp = System.currentTimeMillis();

    performWait(fetchSinceRunnable(session, timestamp + 2000, new String[] {}));
    dispose(session);
  }
  
  protected void fetchOneRecordByGuid(Record record0, Record record1) {
    RepositorySession session = createAndBeginSession();
    
    Record[] store = new Record[] { record0, record1 };
    performWait(storeManyRunnable(session, store));

    String[] guids = new String[] { record0.guid };
    Record[] expected = new Record[] { record0 };
    performWait(fetchRunnable(session, guids, expected));
    dispose(session);
  }
  
  protected void fetchMultipleRecordsByGuids(Record record0,
      Record record1, Record record2) {
    RepositorySession session = createAndBeginSession();

    Record[] store = new Record[] { record0, record1, record2 };
    performWait(storeManyRunnable(session, store));
    
    String[] guids = new String[] { record0.guid, record2.guid };
    Record[] expected = new Record[] { record0, record2 };
    performWait(fetchRunnable(session, guids, expected));
    dispose(session);
  }
  
  protected void fetchNoRecordByGuid(Record record) {
    RepositorySession session = createAndBeginSession();
    
    performWait(storeRunnable(session, record));
    performWait(fetchRunnable(session,
                              new String[] { Utils.generateGuid() }, 
                              new Record[] {}));
    dispose(session);
  }
  
  /*
   * Test wipe
   */
  protected void doWipe(final Record record0, final Record record1) {
    final RepositorySession session = createAndBeginSession();
    final Runnable run = new Runnable() {
      @Override
      public void run() {
        session.wipe(new RepositorySessionWipeDelegate() {
          public void onWipeSucceeded() {
            performNotify();
          }
          public void onWipeFailed(Exception ex) {
            performNotify("Wipe should have succeeded", ex);
          }
          @Override
          public RepositorySessionWipeDelegate deferredWipeDelegate(final ExecutorService executor) {
            final RepositorySessionWipeDelegate self = this;
            return new RepositorySessionWipeDelegate() {

              @Override
              public void onWipeSucceeded() {
                new Thread(new Runnable() {
                  @Override
                  public void run() {
                    self.onWipeSucceeded();
                  }}).start();
              }

              @Override
              public void onWipeFailed(final Exception ex) {
                new Thread(new Runnable() {
                  @Override
                  public void run() {
                    self.onWipeFailed(ex);
                  }}).start();
              }

              @Override
              public RepositorySessionWipeDelegate deferredWipeDelegate(ExecutorService newExecutor) {
                if (newExecutor == executor) {
                  return this;
                }
                throw new IllegalArgumentException("Can't re-defer this delegate.");
              }
            };
          }
        });
      }
    };

    // Store 2 records.
    Record[] records = new Record[] { record0, record1 };
    performWait(storeManyRunnable(session, records));
    performWait(fetchAllRunnable(session, records));

    // Wipe.
    performWait(run);
    dispose(session);
  }
  
  /*
   * TODO adding or subtracting from lastModified timestamps does NOTHING
   * since it gets overwritten when we store stuff. See other tests
   * for ways to do this properly.
   */

  /*
   * Record being stored has newer timestamp than existing local record, local
   * record has not been modified since last sync.
   */
  protected void remoteNewerTimeStamp(Record local, Record remote) {
    final RepositorySession session = createAndBeginSession();

    // Record existing and hasn't changed since before lastSync.
    // Automatically will be assigned lastModified = current time.
    performWait(storeRunnable(session, local));

    remote.guid = local.guid;
    
    // Get the timestamp and make remote newer than it
    ExpectFetchDelegate timestampDelegate = preparedExpectFetchDelegate(new Record[] { local });
    performWait(fetchRunnable(session, new String[] { remote.guid }, timestampDelegate));
    remote.lastModified = timestampDelegate.records.get(0).lastModified + 1000;
    performWait(storeRunnable(session, remote));

    Record[] expected = new Record[] { remote };
    ExpectFetchDelegate delegate = preparedExpectFetchDelegate(expected);
    performWait(fetchAllRunnable(session, delegate));
    dispose(session);
  }

  /*
   * Local record has a newer timestamp than the record being stored. For now,
   * we just take newer (local) record)
   */
  protected void localNewerTimeStamp(Record local, Record remote) {
    final RepositorySession session = createAndBeginSession();

    performWait(storeRunnable(session, local));

    remote.guid = local.guid;
    
    // Get the timestamp and make remote older than it
    ExpectFetchDelegate timestampDelegate = preparedExpectFetchDelegate(new Record[] { local });
    performWait(fetchRunnable(session, new String[] { remote.guid }, timestampDelegate));
    remote.lastModified = timestampDelegate.records.get(0).lastModified - 1000;
    performWait(storeRunnable(session, remote));
    
    // Do a fetch and make sure that we get back the local record.
    Record[] expected = new Record[] { local };
    performWait(fetchAllRunnable(session, preparedExpectFetchDelegate(expected)));
    dispose(session);
  }
  
  /*
   * Insert a record that is marked as deleted, remote has newer timestamp
   */
  protected void deleteRemoteNewer(Record local, Record remote) {
    final RepositorySession session = createAndBeginSession();
    
    // Record existing and hasn't changed since before lastSync.
    // Automatically will be assigned lastModified = current time.
    performWait(storeRunnable(session, local));

    // Pass the same record to store, but mark it deleted and modified
    // more recently
    ExpectFetchDelegate timestampDelegate = preparedExpectFetchDelegate(new Record[] { local });
    performWait(fetchRunnable(session, new String[] { local.guid }, timestampDelegate));
    remote.lastModified = timestampDelegate.records.get(0).lastModified + 1000;
    remote.deleted = true;
    remote.guid = local.guid;
    performWait(storeRunnable(session, remote));

    performWait(fetchAllRunnable(session, preparedExpectFetchDelegate(new Record[]{})));
    dispose(session);
  }
  
  // Store two records that are identical (this has different meanings based on the
  // type of record) other than their guids. The record existing locally already
  // should have its guid replaced (the assumption is that the record existed locally
  // and then sync was enabled and this record existed on another sync'd device).
  public void storeIdenticalExceptGuid(Record record0) {
    Log.i("storeIdenticalExceptGuid", "Started.");
    final RepositorySession session = createAndBeginSession();
    Log.i("storeIdenticalExceptGuid", "Session is " + session);
    performWait(storeRunnable(session, record0));
    Log.i("storeIdenticalExceptGuid", "Stored record0.");
    DefaultFetchDelegate timestampDelegate = getTimestampDelegate(record0.guid);

    performWait(fetchRunnable(session, new String[] { record0.guid }, timestampDelegate));
    Log.i("storeIdenticalExceptGuid", "fetchRunnable done.");
    record0.lastModified = timestampDelegate.records.get(0).lastModified + 3000;
    record0.guid = Utils.generateGuid();
    Log.i("storeIdenticalExceptGuid", "Storing modified...");
    performWait(storeRunnable(session, record0));
    Log.i("storeIdenticalExceptGuid", "Stored modified.");
    
    Record[] expected = new Record[] { record0 };
    performWait(fetchAllRunnable(session, preparedExpectFetchDelegate(expected)));
    Log.i("storeIdenticalExceptGuid", "Fetched all. Returning.");
    dispose(session);
  }
  
  // Special delegate so that we don't verify parenting is correct since
  // at some points it won't be since parent folder hasn't been stored.
  private DefaultFetchDelegate getTimestampDelegate(final String guid) {
    return new DefaultFetchDelegate() {
      @Override
      public void onFetchCompleted(final long fetchEnd) {
        assertEquals(guid, this.records.get(0).guid);
        performNotify();
      }
    };
  }
  
  /*
   * Insert a record that is marked as deleted, local has newer timestamp
   * and was not marked deleted (so keep it)
   */
  protected void deleteLocalNewer(Record local, Record remote) {
    Log.d("deleteLocalNewer", "Begin.");
    final RepositorySession session = createAndBeginSession();

    Log.d("deleteLocalNewer", "Storing local...");
    performWait(storeRunnable(session, local));

    // Create an older version of a record with the same GUID.
    remote.guid = local.guid;

    Log.d("deleteLocalNewer", "Fetching...");

    // Get the timestamp and make remote older than it
    Record[] expected = new Record[] { local };
    ExpectFetchDelegate timestampDelegate = preparedExpectFetchDelegate(expected);
    performWait(fetchRunnable(session, new String[] { remote.guid }, timestampDelegate));

    Log.d("deleteLocalNewer", "Fetched.");
    remote.lastModified = timestampDelegate.records.get(0).lastModified - 1000;

    Log.d("deleteLocalNewer", "Last modified is " + remote.lastModified);
    remote.deleted = true;
    Log.d("deleteLocalNewer", "Storing deleted...");
    performWait(quietStoreRunnable(session, remote));      // This appears to do a lot of work...?!
    Log.d("deleteLocalNewer", "Stored deleted.");

    // Do a fetch and make sure that we get back the first (local) record.
    performWait(fetchAllRunnable(session, preparedExpectFetchDelegate(expected)));
    Log.d("deleteLocalNewer", "Fetched and done!");
    dispose(session);
  }
  
  /*
   * Insert a record that is marked as deleted, record never existed locally
   */
  protected void deleteRemoteLocalNonexistent(Record remote) {
    final RepositorySession session = createAndBeginSession();
    
    long timestamp = 1000000000;
    
    // Pass a record marked deleted to store, doesn't exist locally
    remote.lastModified = timestamp;
    remote.deleted = true;
    performWait(quietStoreRunnable(session, remote));

    ExpectFetchDelegate delegate = preparedExpectFetchDelegate(new Record[]{});
    performWait(fetchAllRunnable(session, delegate));
    dispose(session);
  }
  
  /*
   * Tests that don't require specific records based on type of repository.
   * These tests don't need to be overriden in subclasses, they will just work.
   */
  public void testCreateSessionNullContext() {
    Log.i(LOG_TAG, "In testCreateSessionNullContext.");
    AndroidBrowserRepository repo = getRepository();
    try {
      repo.createSession(new DefaultSessionCreationDelegate(), null);
      fail("Should throw.");
    } catch (Exception ex) {
      assertNotNull(ex);
    }
  }
  
  public void testStoreNullRecord() {
    final RepositorySession session = createAndBeginSession();
    try {
      session.setStoreDelegate(new DefaultStoreDelegate());
      session.store(null);
      fail("Should throw.");
    } catch (Exception ex) {
      assertNotNull(ex);
    }
    dispose(session);
  }
  
  public void testFetchNoGuids() {
    final RepositorySession session = createAndBeginSession();
    performWait(fetchRunnable(session, new String[] {}, new ExpectInvalidRequestFetchDelegate()));
    dispose(session);
  }
  
  public void testFetchNullGuids() {
    final RepositorySession session = createAndBeginSession();
    performWait(fetchRunnable(session, null, new ExpectInvalidRequestFetchDelegate()));
    dispose(session);
  }
  
  public void testBeginOnNewSession() {
    final RepositorySession session = createSession();
    performWait(beginRunnable(session, new ExpectBeginDelegate()));
    dispose(session);
  }
  
  public void testBeginOnRunningSession() {
    final RepositorySession session = createAndBeginSession();
    try {
      session.begin(new ExpectBeginFailDelegate());
    } catch (InvalidSessionTransitionException e) {
      dispose(session);
      return;
    }
    fail("Should have caught InvalidSessionTransitionException.");
  }
  
  public void testBeginOnFinishedSession() throws InactiveSessionException {
    final RepositorySession session = createAndBeginSession();
    performWait(finishRunnable(session, new ExpectFinishDelegate()));
    try {
      session.begin(new ExpectBeginFailDelegate());
    } catch (InvalidSessionTransitionException e) {
      Log.i(getName(), "Yay! Got an exception.", e);
      dispose(session);
      return;
    } catch (Exception e) {
      Log.i(getName(), "Yay! Got an exception.", e);
      dispose(session);
      return;
    }
    fail("Should have caught InvalidSessionTransitionException.");
  }
  
  public void testFinishOnFinishedSession() throws InactiveSessionException {
    final RepositorySession session = createAndBeginSession();
    performWait(finishRunnable(session, new ExpectFinishDelegate()));
    try {
      session.finish(new ExpectFinishFailDelegate());
    } catch (InactiveSessionException e) {
      dispose(session);
      return;
    }
    fail("Should have caught InactiveSessionException.");
  }
  
  public void testFetchOnInactiveSession() throws InactiveSessionException {
    final RepositorySession session = createSession();
    try {
      session.fetch(new String[] { Utils.generateGuid() }, new DefaultFetchDelegate());
    } catch (InactiveSessionException e) {
      // Yay.
      dispose(session);
      return;
    };
    fail("Should have caught InactiveSessionException.");
  }

  public void testFetchOnFinishedSession() {
    final RepositorySession session = createAndBeginSession();
    Log.i(getName(), "Finishing...");
    performWait(finishRunnable(session, new ExpectFinishDelegate()));
    try {
      session.fetch(new String[] { Utils.generateGuid() }, new DefaultFetchDelegate());
    } catch (InactiveSessionException e) {
      // Yay.
      dispose(session);
      return;
    };
    fail("Should have caught InactiveSessionException.");
  }
  
  public void testGuidsSinceOnUnstartedSession() {
    final RepositorySession session = createSession();
    Runnable run = new Runnable() {
      @Override
      public void run() {
        session.guidsSince(System.currentTimeMillis(),
            new RepositorySessionGuidsSinceDelegate() {
              public void onGuidsSinceSucceeded(String[] guids) {
                performNotify("Session inactive, should fail", null);
              }

              public void onGuidsSinceFailed(Exception ex) {
                verifyInactiveException(ex);
                performNotify();
              }
            });
      }
    };
    performWait(run);
    dispose(session);
  }

  private void verifyInactiveException(Exception ex) {
    if (!(ex instanceof InactiveSessionException)) {
      fail("Wrong exception type");
    }
  }
}
