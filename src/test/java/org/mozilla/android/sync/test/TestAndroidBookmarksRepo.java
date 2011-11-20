/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.android.sync.MainActivity;
import org.mozilla.android.sync.repositories.BookmarksRepository;
import org.mozilla.android.sync.repositories.BookmarksRepositorySession;
import org.mozilla.android.sync.repositories.RepoStatusCode;
import org.mozilla.android.sync.repositories.RepositorySession;
import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.repositories.domain.Record;
import org.mozilla.android.sync.test.helpers.BookmarkHelpers;
import org.mozilla.android.sync.test.helpers.DefaultSessionCreationDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFetchAllDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFetchGUIDsDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFetchSinceDelegate;
import org.mozilla.android.sync.test.helpers.ExpectNoGUIDsSinceDelegate;
import org.mozilla.android.sync.test.helpers.ExpectStoredDelegate;
import org.mozilla.android.sync.test.helpers.WaitHelper;

import android.content.Context;

import com.xtremelabs.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TestAndroidBookmarksRepo {

  // Store these globally for convenience.
  public static WaitHelper testWaiter = WaitHelper.getTestWaiter();
  public static BookmarksRepositorySession session;
  
  private class SetupDelegate extends DefaultSessionCreationDelegate {
    public void onSessionCreated(RepositorySession sess) {
      AssertionError err = null;
      try {
        assertNotNull(sess);
        session = (BookmarksRepositorySession) sess;
      } catch (AssertionError e) {
        err = e;
      }
      testWaiter.performNotify(err);
    }
  }
  
  public BookmarksRepository prepareRepositorySession(DefaultSessionCreationDelegate delegate, long lastSyncTimestamp) {
    BookmarksRepository repository = new BookmarksRepository();
    
    Context context = new MainActivity().getApplicationContext();
    repository.createSession(context, delegate, lastSyncTimestamp);
    testWaiter.performWait();
    return repository;
  }

  protected void prepEmptySession() {
    this.prepareRepositorySession(new SetupDelegate(), 0);
    
    // Ensure there are no records.
    session.guidsSince(0, new ExpectNoGUIDsSinceDelegate());
    testWaiter.performWait();    
  }

  /*
   * Tests for createSession.
   */
  @Test
  public void testCreateSessionNullContext() {
    BookmarksRepository repo = new BookmarksRepository();
    try {
      repo.createSession(null, new DefaultSessionCreationDelegate(), 0);
      fail("Should throw.");
    } catch (Exception ex) {
      assertNotNull(ex);
    }
  }

  @Test
  public void testFetchAll() {
    this.prepareRepositorySession(new SetupDelegate(), 0);
    Record[] expected = new Record[2];
    String[] expectedGUIDs = new String[2];
    expected[0] = BookmarkHelpers.createBookmark1();
    expected[1] = BookmarkHelpers.createBookmark2();
    expectedGUIDs[0] = expected[0].getGUID();
    expectedGUIDs[1] = expected[1].getGUID();
    ExpectFetchAllDelegate delegate = new ExpectFetchAllDelegate(expectedGUIDs);
    session.store(expected[0], new ExpectStoredDelegate());
    testWaiter.performWait();
    session.store(expected[1], new ExpectStoredDelegate());
    testWaiter.performWait();
    session.fetchAll(delegate);
    testWaiter.performWait();
    assertEquals(delegate.records.length, 2);
    assertEquals(delegate.code, RepoStatusCode.DONE);
  }

  /*
   * Tests for fetching GUIDs since a timestamp.
   */
  @Test
  public void testGuidsSinceReturnMultipleRecords() {
    prepEmptySession();
    long timestamp = System.currentTimeMillis();

    //  Store 2 records in the future.
    BookmarkRecord record0 = BookmarkHelpers.createLivemark();
    BookmarkRecord record1 = BookmarkHelpers.createMicrosummary();

    String[] expected = new String[2];
    expected[0] = record0.getGUID();
    expected[1] = record1.getGUID();
    record0.setLastModified(timestamp + 1000);
    record1.setLastModified(timestamp + 1500);

    session.store(record0, new ExpectStoredDelegate(expected[0]));
    testWaiter.performWait();
    session.store(record1, new ExpectStoredDelegate(expected[1]));
    testWaiter.performWait();

    session.guidsSince(timestamp, new ExpectFetchGUIDsDelegate(expected));
    testWaiter.performWait();
  }

  @Test
  public void testGuidsSinceReturnNoRecords() {
    prepEmptySession();
    long timestamp = System.currentTimeMillis();

    //  Store 1 record in the past.
    BookmarkRecord record0 = BookmarkHelpers.createLivemark();
    record0.setLastModified(timestamp - 1000);
    session.store(record0, new ExpectStoredDelegate(record0.getGUID()));
    testWaiter.performWait();

    String[] expected = {};
    session.guidsSince(timestamp, new ExpectFetchGUIDsDelegate(expected));
    testWaiter.performWait();
  }

  /*
   * Tests for fetchSince
   */
  @Test
  public void testFetchSinceOneRecord() {
    prepEmptySession();
    long timestamp = System.currentTimeMillis();

    // Store a folder.
    BookmarkRecord folder = BookmarkHelpers.createFolder();
    folder.setLastModified(timestamp);       // Verify inclusive retrieval.
    session.store(folder, new ExpectStoredDelegate(folder.getGUID()));
    testWaiter.performWait();
    
    // Store a bookmark.
    BookmarkRecord bookmark = BookmarkHelpers.createBookmark2();
    bookmark.setLastModified(timestamp + 3000);
    session.store(bookmark, new ExpectStoredDelegate(bookmark.getGUID()));
    testWaiter.performWait();
    
    // Fetch just the bookmark.
    String[] expectedOne = new String[1];
    expectedOne[0] = bookmark.getGUID();
    session.fetchSince(timestamp + 1, new ExpectFetchSinceDelegate(timestamp, expectedOne));
    testWaiter.performWait();
    
    // Fetch both, relying on inclusiveness.
    String[] expectedBoth = new String[2];
    expectedBoth[0] = folder.getGUID();
    expectedBoth[1] = bookmark.getGUID();
    session.fetchSince(timestamp, new ExpectFetchSinceDelegate(timestamp, expectedBoth));
    testWaiter.performWait();
  }

  //////////
  ////////// TODO: forward-port these tests!
  //////////

//
//  @Test
//  public void testFetchSinceReturnNoRecords() {
//
//    // Create a record and store it
//    CallbackResult result = testWrapper.doStoreSync(session, BookmarkHelpers.createBookmark1());
//    BookmarkHelpers.verifyStoreResult(result);
//
//    // Wait 2 seconds
//    perform2SecondWait();
//    long timestamp = System.currentTimeMillis()/1000;
//
//    // Get records
//    result = testWrapper.doFetchSinceSync(session, timestamp);
//
//    // Verify that no guids come back
//    assertEquals(0, result.getRecords().length);
//    BookmarkHelpers.verifyFetchSince(result);
//  }
//
//  /*
//   * Tests for fetch(guid)
//   */
//  @Test
//  public void testFetchOneRecordByGuid() {
//    // Create two records and store them
//    BookmarkRecord record = BookmarkHelpers.createBookmark1();
//    String guid = record.getGuid();
//    CallbackResult result = testWrapper.doStoreSync(session, record);
//    BookmarkHelpers.verifyStoreResult(result);
//    result = testWrapper.doStoreSync(session, BookmarkHelpers.createBookmark2());
//    BookmarkHelpers.verifyStoreResult(result);
//
//    // Fetch record with guid from above and ensure we only get back one record
//    result = testWrapper.doFetchSync(session, new String[] { guid });
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
//  @Test
//  public void testFetchMultipleRecordsByGuid() {
//    // Create three records and store them
//    BookmarkRecord record = BookmarkHelpers.createBookmark1();
//    BookmarkRecord record2 = BookmarkHelpers.createQuery();
//    BookmarkRecord record3 = BookmarkHelpers.createSeparator();
//    CallbackResult result = testWrapper.doStoreSync(session, record);
//    BookmarkHelpers.verifyStoreResult(result);
//    result = testWrapper.doStoreSync(session, record2);
//    BookmarkHelpers.verifyStoreResult(result);
//    result = testWrapper.doStoreSync(session, record3);
//    BookmarkHelpers.verifyStoreResult(result);
//
//    // Fetch records with 2 guids from above
//    result = testWrapper.doFetchSync(session, new String[] { record.getGuid(), record3.getGuid() });
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
//  @Test
//  public void testFetchNoRecordByGuid() {
//    // Create a record and store it
//    CallbackResult result = testWrapper.doStoreSync(session, BookmarkHelpers.createMicrosummary());
//    BookmarkHelpers.verifyStoreResult(result);
//
//    // Fetch a record that doesn't exist
//    result = testWrapper.doFetchSync(session, new String[] { Utils.generateGuid() });
//
//    // Ensure no recrods are returned
//    assertEquals(0, result.getRecords().length);
//    BookmarkHelpers.verifyFetch(result);
//  }
//
//  @Test
//  public void testFetchNoGuids() {
//
//    // Fetch with empty guids list
//    CallbackResult result = testWrapper.doFetchSync(session, new String[] { });
//
//    // Ensure no records are returned
//    assertEquals(RepoStatusCode.INVALID_REQUEST, result.getStatusCode());
//    assertEquals(0, result.getRecords().length);
//    assertEquals(CallType.FETCH, result.getCallType());
//  }
//
//  @Test
//  public void testFetchNullGuids() {
//
//    // Fetch with empty guids list
//    CallbackResult result = testWrapper.doFetchSync(session, null);
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