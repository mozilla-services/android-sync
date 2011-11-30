/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.MainActivity;
import org.mozilla.android.sync.repositories.InactiveSessionException;
import org.mozilla.android.sync.repositories.Utils;
import org.mozilla.android.sync.repositories.bookmarks.BookmarksDatabaseHelper;
import org.mozilla.android.sync.repositories.bookmarks.BookmarksRepository;
import org.mozilla.android.sync.repositories.bookmarks.BookmarksRepositorySession;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionFetchRecordsDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionGuidsSinceDelegate;
import org.mozilla.android.sync.repositories.delegates.RepositorySessionWipeDelegate;
import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.repositories.domain.Record;
import org.mozilla.android.sync.test.helpers.BookmarkHelpers;
import org.mozilla.android.sync.test.helpers.DefaultSessionCreationDelegate;
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

public class AndroidBookmarksRepoTest extends ActivityInstrumentationTestCase2<MainActivity> {
  public AndroidBookmarksRepoTest() {
    super(MainActivity.class);
  }

  public Context getApplicationContext() {
    return this.getInstrumentation().getTargetContext().getApplicationContext();
  }

  private void performWait() {
    AndroidBookmarksTestHelper.testWaiter.performWait();
  }
  private BookmarksRepositorySession getSession() {
    return AndroidBookmarksTestHelper.session;
  }

  private void prepEmptySession() {
    AndroidBookmarksTestHelper.prepEmptySession(getApplicationContext());
  }
  
  private void prepEmptySessionWithoutBegin() {
    AndroidBookmarksTestHelper.prepEmptySessionWithoutBegin(getApplicationContext());
  }
  
  private static BookmarksDatabaseHelper helper;

  private void wipe() {
    if (helper == null) {
      helper = new BookmarksDatabaseHelper(getApplicationContext());
    }
    helper.wipe();
  }

  public void setUp() {
    Log.i("rnewman", "Wiping.");
    wipe();
  }
  public void tearDown() {
    if (helper != null) {
      helper.close();
    }
  }

  /*
   * Tests for createSession.
   */
  public void testCreateSessionNullContext() {
    Log.i("rnewman", "In testCreateSessionNullContext.");
    BookmarksRepository repo = new BookmarksRepository();
    try {
      repo.createSession(null, new DefaultSessionCreationDelegate(), 0);
      fail("Should throw.");
    } catch (Exception ex) {
      assertNotNull(ex);
    }
  }

  public void testFetchAll() {
    Log.i("rnewman", "Starting testFetchAll.");
    AndroidBookmarksTestHelper.prepareRepositorySession(getApplicationContext(), new SetupDelegate(), 0, true);
    Log.i("rnewman", "Prepared.");
    Record[] expected = new Record[2];
    String[] expectedGUIDs = new String[2];
    expected[0] = BookmarkHelpers.createBookmark1();
    expected[1] = BookmarkHelpers.createBookmark2();
    expectedGUIDs[0] = expected[0].guid;
    expectedGUIDs[1] = expected[1].guid;

    BookmarksRepositorySession session = getSession();
    session.store(expected[0], new ExpectStoredDelegate(expected[0].guid));
    performWait();
    session.store(expected[1], new ExpectStoredDelegate(expected[1].guid));
    performWait();   
    
    ExpectFetchAllDelegate delegate = new ExpectFetchAllDelegate(expectedGUIDs);
    session.fetchAll(delegate);
    performWait();

    assertEquals(delegate.recordCount(), 2);
  }

  /*
   * Tests for fetching GUIDs since a timestamp.
   */
  public void testGuidsSinceReturnMultipleRecords() {
    prepEmptySession();
    long timestamp = System.currentTimeMillis();

    //  Store 2 records in the future.
    BookmarkRecord record0 = BookmarkHelpers.createLivemark();
    BookmarkRecord record1 = BookmarkHelpers.createMicrosummary();

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
  
  public void testGuidsSinceReturnNoRecords() {
    prepEmptySession();
    long timestamp = System.currentTimeMillis();

    //  Store 1 record in the past.
    BookmarkRecord record0 = BookmarkHelpers.createLivemark();
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
  public void testFetchSinceOneRecord() {
    prepEmptySession();
    long timestamp = System.currentTimeMillis();

    // Store a folder.
    BookmarkRecord folder = BookmarkHelpers.createFolder();
    folder.lastModified = timestamp;       // Verify inclusive retrieval.
    getSession().store(folder, new ExpectStoredDelegate(folder.guid));
    performWait();

    // Store a bookmark.
    BookmarkRecord bookmark = BookmarkHelpers.createBookmark2();
    bookmark.lastModified = timestamp + 3000;
    getSession().store(bookmark, new ExpectStoredDelegate(bookmark.guid));
    performWait();

    // Fetch just the bookmark.
    String[] expectedOne = new String[1];
    expectedOne[0] = bookmark.guid;
    getSession().fetchSince(timestamp + 1, new ExpectFetchSinceDelegate(timestamp, expectedOne));
    performWait();

    // Fetch both, relying on inclusiveness.
    String[] expectedBoth = new String[2];
    expectedBoth[0] = folder.guid;
    expectedBoth[1] = bookmark.guid;
    getSession().fetchSince(timestamp, new ExpectFetchSinceDelegate(timestamp, expectedBoth));
    performWait();
  }

  public void testFetchSinceReturnNoRecords() {
    prepEmptySession();
    BookmarkRecord record0 = BookmarkHelpers.createBookmark1();
    
    getSession().store(record0, new ExpectStoredDelegate(record0.guid));
    performWait();

    long timestamp = System.currentTimeMillis()/1000;

    getSession().fetchSince(timestamp, new ExpectFetchSinceDelegate(timestamp, new String[] { }));
    performWait();
  }

  /*
   * Tests for fetch(guid)
   */
  
  public void testFetchOneRecordByGuid() {
    prepEmptySession();
    BookmarkRecord record0 = BookmarkHelpers.createBookmark1();
    BookmarkRecord record1 = BookmarkHelpers.createBookmark2();
    
    getSession().store(record0, new ExpectStoredDelegate(record0.guid));
    performWait();
    getSession().store(record1, new ExpectStoredDelegate(record1.guid));
    performWait();
    
    String[] expected = new String[] { record0.guid };
    getSession().fetch(expected, new ExpectFetchDelegate(expected));
    performWait();
  }
  
  public void testFetchMultipleRecordsByGuids() {
    prepEmptySession();
    BookmarkRecord record0 = BookmarkHelpers.createBookmark1();
    BookmarkRecord record1 = BookmarkHelpers.createBookmark2();
    BookmarkRecord record2 = BookmarkHelpers.createQuery();

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
  
  public void testFetchNoRecordByGuid() {
    prepEmptySession();
    
    BookmarkRecord record0 = BookmarkHelpers.createSeparator();
    getSession().store(record0, new ExpectStoredDelegate(record0.guid));
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
   * Test begin/finish
   */
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
      }
      public void onFetchFailed(Exception ex) {
        verifyInactiveException(ex);
      }
    });
  }
  
  public void testGuidsSinceOnUnstartedSession() {
    prepEmptySessionWithoutBegin();
    getSession().guidsSince(System.currentTimeMillis(), new RepositorySessionGuidsSinceDelegate() {
      public void onGuidsSinceSucceeded(String[] guids) {
        fail("Session inactive, should fail");
      }
      public void onGuidsSinceFailed(Exception ex) {
        verifyInactiveException(ex);
      }
    });
  }
  
  private void verifyInactiveException(Exception ex) {
    if (ex.getClass() != InactiveSessionException.class) {
      fail("Wrong exception type");
    }
  }
  
  /*
   * Test wipe
   */
  public void testWipe() {
    prepEmptySession();
    
    BookmarkRecord record0 = BookmarkHelpers.createFolder();
    BookmarkRecord record1 = BookmarkHelpers.createBookmark2();
    
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
        //no-op: Passes
      }
      public void onWipeFailed(Exception ex) {
        fail("wipe should have succeeded");
      }
    });
  }

}