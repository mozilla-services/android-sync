package org.mozilla.android.sync.test;

import org.mozilla.android.sync.MainActivity;
import org.mozilla.android.sync.repositories.Utils;
import org.mozilla.android.sync.repositories.android.AndroidBrowserRepository;
import org.mozilla.android.sync.repositories.android.AndroidBrowserRepositoryDatabaseHelper;
import org.mozilla.android.sync.repositories.android.AndroidBrowserRepositorySession;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.android.sync.repositories.domain.Record;
import org.mozilla.android.sync.test.helpers.ExpectFetchAllDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFetchDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFetchSinceDelegate;
import org.mozilla.android.sync.test.helpers.ExpectGuidsSinceDelegate;
import org.mozilla.android.sync.test.helpers.ExpectInvalidRequestFetchDelegate;
import org.mozilla.android.sync.test.helpers.ExpectStoredDelegate;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

public abstract class AndroidRepositoryTest extends ActivityInstrumentationTestCase2<MainActivity> {
  
  protected AndroidBrowserRepositoryDatabaseHelper helper;
  
  public AndroidRepositoryTest() {
    super(MainActivity.class);
  }

  public Context getApplicationContext() {
    return this.getInstrumentation().getTargetContext().getApplicationContext();
  }

  protected void performWait(Runnable runnable) throws AssertionError {
    AndroidRepositoryTestHelper.testWaiter.performWait(runnable);
  }
  
  // TODO move away from these and use only runnables
  protected void performWait() {
    AndroidRepositoryTestHelper.testWaiter.performWait();
  }
  protected void performNotfiy() {
    AndroidRepositoryTestHelper.testWaiter.performNotify();
  }

  protected AndroidBrowserRepositorySession getSession() {
    return AndroidRepositoryTestHelper.session;
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
    AndroidRepositoryTestHelper.prepareRepositorySession(getApplicationContext(),
        new SetupDelegate(), 0, true, getRepository());
    // Clear old data.
    wipe();
  }
  
  // TODO consider getting rid of AndroidRepositoryTestHelper and moving it into here???
  protected void prepEmptySession() {
    AndroidRepositoryTestHelper.prepEmptySession(getApplicationContext(), getRepository());
  }
  
  protected void prepEmptySessionWithoutBegin() {
    AndroidRepositoryTestHelper.prepEmptySessionWithoutBegin(getApplicationContext(), getRepository());
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
  
  /*
   * Test abstractions...may require organizing into multiple files later
   */
  // TODO if we restructure where these live, we might be able to force subclasses
  // to implement these tests via abstract methods (can't do this right now due to layout)
  protected void basicStoreTest(Record record) {
    prepSession();
    Runnable runnable = getStoreRunnable(record, new ExpectStoredDelegate(record.guid));
    performWait(runnable);
  }
  
  protected void basicFetchAllTest(Record[] expected) {
    Log.i("rnewman", "Starting testFetchAll.");
    AndroidRepositoryTestHelper.prepareRepositorySession(getApplicationContext(),
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
  
}