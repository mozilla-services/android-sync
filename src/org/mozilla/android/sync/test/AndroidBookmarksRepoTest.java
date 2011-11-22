/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.MainActivity;
import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.bookmarks.BookmarksDatabaseHelper;
import org.mozilla.android.sync.repositories.bookmarks.BookmarksRepository;
import org.mozilla.android.sync.repositories.bookmarks.BookmarksRepositorySession;
import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.repositories.domain.Record;
import org.mozilla.android.sync.test.helpers.BookmarkHelpers;
import org.mozilla.android.sync.test.helpers.DefaultSessionCreationDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFetchAllDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFetchGUIDsDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFetchSinceDelegate;
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
   // wipe();
    AndroidBookmarksTestHelper.prepEmptySession(getApplicationContext());
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
//    wipe();
    Log.i("rnewman", "Starting testFetchAll.");
    AndroidBookmarksTestHelper.prepareRepositorySession(getApplicationContext(), new SetupDelegate(), 0);
    Log.i("rnewman", "Prepared.");
    Record[] expected = new Record[2];
    String[] expectedGUIDs = new String[2];
    expected[0] = BookmarkHelpers.createBookmark1();
    expected[1] = BookmarkHelpers.createBookmark2();
    expectedGUIDs[0] = expected[0].guid;
    expectedGUIDs[1] = expected[1].guid;

    BookmarksRepositorySession session = getSession();
    session.store(expected[0], new ExpectStoredDelegate());
    performWait();
    session.store(expected[1], new ExpectStoredDelegate());
    performWait();   
    
    ExpectFetchAllDelegate delegate = new ExpectFetchAllDelegate(expectedGUIDs);
    session.fetchAll(delegate);
    performWait();

    assertEquals(delegate.recordCount(), 2);
    assertEquals(delegate.code, RepoStatusCode.DONE);
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

    getSession().guidsSince(timestamp, new ExpectFetchGUIDsDelegate(expected));
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
    getSession().guidsSince(timestamp, new ExpectFetchGUIDsDelegate(expected));
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

  //////////
  ////////// TODO: forward-port these tests!
  //////////

//
//  
//  public void testFetchSinceReturnNoRecords() {
//
//    // Create a record and store it
//    CallbackResult result = testWrapper.doStoreSync(getSession(), BookmarkHelpers.createBookmark1());
//    BookmarkHelpers.verifyStoreResult(result);
//
//    // Wait 2 seconds
//    perform2SecondWait();
//    long timestamp = System.currentTimeMillis()/1000;
//
//    // Get records
//    result = testWrapper.doFetchSinceSync(getSession(), timestamp);
//
//    // Verify that no guids come back
//    assertEquals(0, result.getRecords().length);
//    BookmarkHelpers.verifyFetchSince(result);
//  }
//
//  /*
//   * Tests for fetch(guid)
//   */
//  
//  public void testFetchOneRecordByGuid() {
//    // Create two records and store them
//    BookmarkRecord record = BookmarkHelpers.createBookmark1();
//    String guid = record.getGuid();
//    CallbackResult result = testWrapper.doStoreSync(getSession(), record);
//    BookmarkHelpers.verifyStoreResult(result);
//    result = testWrapper.doStoreSync(getSession(), BookmarkHelpers.createBookmark2());
//    BookmarkHelpers.verifyStoreResult(result);
//
//    // Fetch record with guid from above and ensure we only get back one record
//    result = testWrapper.doFetchSync(getSession(), new String[] { guid });
//
//    // Check that only one record was returned and that it is the correct one
//    Record[] returnedRecords = result.getRecords();
//    assertEquals(1, returnedRecords.length);
//    BookmarkRecord fetched = (BookmarkRecord) returnedRecords[0];
//    assertEquals(guid, fetched.getGuid());
//    BookmarkHelpers.verifyExpectedRecordReturned(record, fetched);
//    BookmarkHelpers.verifyFetch(result);
//  }
//
//  
//  public void testFetchMultipleRecordsByGuid() {
//    // Create three records and store them
//    BookmarkRecord record = BookmarkHelpers.createBookmark1();
//    BookmarkRecord record2 = BookmarkHelpers.createQuery();
//    BookmarkRecord record3 = BookmarkHelpers.createSeparator();
//    CallbackResult result = testWrapper.doStoreSync(getSession(), record);
//    BookmarkHelpers.verifyStoreResult(result);
//    result = testWrapper.doStoreSync(getSession(), record2);
//    BookmarkHelpers.verifyStoreResult(result);
//    result = testWrapper.doStoreSync(getSession(), record3);
//    BookmarkHelpers.verifyStoreResult(result);
//
//    // Fetch records with 2 guids from above
//    result = testWrapper.doFetchSync(getSession(), new String[] { record.getGuid(), record3.getGuid() });
//
//    // Check that only one record was returned and that it is the correct one
//    Record[] returnedRecords = result.getRecords();
//    assertEquals(2, returnedRecords.length);
//    BookmarkRecord fetched = (BookmarkRecord) returnedRecords[0];
//    BookmarkRecord fetched2 = (BookmarkRecord) returnedRecords[1];
//    BookmarkHelpers.verifyExpectedRecordReturned(record, fetched);
//    BookmarkHelpers.verifyExpectedRecordReturned(record3, fetched2);
//    BookmarkHelpers.verifyFetch(result);
//  }
//
//  
//  public void testFetchNoRecordByGuid() {
//    // Create a record and store it
//    CallbackResult result = testWrapper.doStoreSync(getSession(), BookmarkHelpers.createMicrosummary());
//    BookmarkHelpers.verifyStoreResult(result);
//
//    // Fetch a record that doesn't exist
//    result = testWrapper.doFetchSync(getSession(), new String[] { Utils.generateGuid() });
//
//    // Ensure no recrods are returned
//    assertEquals(0, result.getRecords().length);
//    BookmarkHelpers.verifyFetch(result);
//  }
//
//  
//  public void testFetchNoGuids() {
//
//    // Fetch with empty guids list
//    CallbackResult result = testWrapper.doFetchSync(getSession(), new String[] { });
//
//    // Ensure no records are returned
//    assertEquals(RepoStatusCode.INVALID_REQUEST, result.getStatusCode());
//    assertEquals(0, result.getRecords().length);
//    assertEquals(CallType.FETCH, result.getCallType());
//  }
//
//  
//  public void testFetchNullGuids() {
//
//    // Fetch with empty guids list
//    CallbackResult result = testWrapper.doFetchSync(getSession(), null);
//
//    // Ensure no records are returned
//    assertEquals(RepoStatusCode.INVALID_REQUEST, result.getStatusCode());
//    assertEquals(0, result.getRecords().length);
//    assertEquals(CallType.FETCH, result.getCallType());
//  }
//
//  /*
//   * Other helpers
//   */
//  private void perform2SecondWait() {
//    try {
//      synchronized(this) {
//        this.wait(2000);
//      }
//    } catch (InterruptedException e) {
//      e.printStackTrace();
//    }
//  }
//
//
//  }

}
