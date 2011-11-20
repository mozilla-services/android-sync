///* Any copyright is dedicated to the Public Domain.
//   http://creativecommons.org/publicdomain/zero/1.0/ */
//
//package org.mozilla.android.sync.test;
//
//import static org.junit.Assert.assertEquals;
//
//import org.junit.Before;
//import org.junit.Test;
//import org.mozilla.android.sync.MainActivity;
//import org.mozilla.android.sync.repositories.BookmarksRepository;
//import org.mozilla.android.sync.repositories.BookmarksRepositorySession;
//import org.mozilla.android.sync.repositories.CollectionType;
//import org.mozilla.android.sync.repositories.RepoStatusCode;
//import org.mozilla.android.sync.repositories.Repository;
//import org.mozilla.android.sync.repositories.Utils;
//import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
//import org.mozilla.android.sync.test.CallbackResult.CallType;
//
//import android.content.Context;
//
//public class TestAndroidBookmarkStore {
//  /*
//   * This test class is dedicated to testing the
//   * store(record) method of RepositorySession.
//   */
//
//  private BookmarksSessionTestWrapper testWrapper;
//  private BookmarksRepositorySession session;
//  private static final long lastSyncTimestamp = System.currentTimeMillis() + 2000;
//
//  @Before
//  public void setUp() {
//
//    // Create a testWrapper instance
//    setTestWrapper(new BookmarksSessionTestWrapper());
//
//    // Create the session used by tests
//    BookmarksRepository repo = (BookmarksRepository) Repository.makeRepository(CollectionType.Bookmarks);
//    Context context = new MainActivity().getApplicationContext();
//    CallbackResult result = getTestWrapper().doCreateSessionSync(repo, context, lastSyncTimestamp);
//
//    // Check that we got a valid session back
//    assertEquals(result.getStatusCode(), RepoStatusCode.DONE);
//    assertEquals(result.getCallType(), CallType.CREATE_SESSION);
//    assert(result.getSession() != null);
//
//    // Set the session
//    setSession((BookmarksRepositorySession) result.getSession());
//
//  }
//
//  /*
//   * Test storing a record for each different type of Bookmark record
//   */
//  @Test
//  public void testStoreBookmark() {
//    CallbackResult result = getTestWrapper().doStoreSync(session, TestUtils.createBookmark1());
//    TestUtils.verifyStoreResult(result);
//  }
//
//  @Test
//  public void testStoreMicrosummary() {
//    CallbackResult result = getTestWrapper().doStoreSync(session, TestUtils.createMicrosummary());
//    TestUtils.verifyStoreResult(result);
//  }
//
//  @Test
//  public void testStoreQuery() {
//    CallbackResult result = getTestWrapper().doStoreSync(session, TestUtils.createQuery());
//    TestUtils.verifyStoreResult(result);
//  }
//
//  @Test
//  public void testStoreFolder() {
//    CallbackResult result = getTestWrapper().doStoreSync(session, TestUtils.createFolder());
//    TestUtils.verifyStoreResult(result);
//  }
//
//  @Test
//  public void testStoreLivemark() {
//    CallbackResult result = getTestWrapper().doStoreSync(session, TestUtils.createLivemark());
//    TestUtils.verifyStoreResult(result);
//  }
//
//  @Test
//  public void testStoreSeparator() {
//    CallbackResult result = getTestWrapper().doStoreSync(session, TestUtils.createSeparator());
//    TestUtils.verifyStoreResult(result);
//  }
//
//  /*
//   * Tests for resolving conflicts where a record with
//   * the given guid already exists
//   */
//
//  /*
//   * Most basic test
//   *
//   * Record being stored has newer timestamp than
//   * existing local record, local record has not
//   * been modified since last sync.
//   */
//  @Test
//  public void testRemoteNewerTimeStamp() {
//    // Record existing and hasn't changed since before lastSync
//    // Automatically will be assigned last mod timestamp of current time
//    // hence why lastSyncTime above is set to future
//    BookmarkRecord local = TestUtils.createBookmark1();
//    local.setAndroidId(54321);
//    CallbackResult result = testWrapper.doStoreSync(session, local);
//    TestUtils.verifyStoreResult(result);
//
//    // Create second bookmark to be passed to store, give it a
//    // last modified timestamp after lastSync and set it as same guid
//    BookmarkRecord remote = TestUtils.createBookmark2();
//    remote.setGuid(local.getGuid());
//    remote.setLastModTime(lastSyncTimestamp + 10000);
//    result = testWrapper.doStoreSync(session, remote);
//    TestUtils.verifyStoreResult(result);
//
//    // Do a fetch and make sure that we get back the second record
//    result = testWrapper.doFetchSync(session, new String[] { local.getGuid() });
//
//    // Check that one record comes back, it is the remote one, and has android ID same as first
//    assertEquals(1, result.getRecords().length);
//    BookmarkRecord record = (BookmarkRecord) (result.getRecords()[0]);
//    TestUtils.verifyExpectedRecordReturned(remote, record);
//    assertEquals(local.getAndroidId(), record.getAndroidId());
//  }
//
//  /*
//   * Local record has a newer timestamp than
//   * record being stored. For now, we just take newer (local) record)
//   */
//  @Test
//  public void testLocalNewerTimeStamp() {
//    // Local record newer
//    BookmarkRecord local = TestUtils.createBookmark1();
//    local.setAndroidId(54321);
//    local.setLastModTime(lastSyncTimestamp + 100000);
//    CallbackResult result = testWrapper.doStoreSync(session, local);
//    TestUtils.verifyStoreResult(result);
//
//    // Create second bookmark to be passed to store, give it a
//    // last modified timestamp before other record and set it as same guid
//    BookmarkRecord remote = TestUtils.createBookmark2();
//    remote.setGuid(local.getGuid());
//    result = testWrapper.doStoreSync(session, remote);
//    TestUtils.verifyStoreResult(result);
//
//    // Do a fetch and make sure that we get back the first (local) record
//    result = testWrapper.doFetchSync(session, new String[] { local.getGuid() });
//
//    // Check that one record comes back, it is the local one
//    assertEquals(1, result.getRecords().length);
//    BookmarkRecord record = (BookmarkRecord) (result.getRecords()[0]);
//    TestUtils.verifyExpectedRecordReturned(local, record);
//    assertEquals(local.getAndroidId(), record.getAndroidId());
//  }
//
//  public BookmarksSessionTestWrapper getTestWrapper() {
//    return testWrapper;
//  }
//
//  public void setTestWrapper(BookmarksSessionTestWrapper testWrapper) {
//    this.testWrapper = testWrapper;
//  }
//
//  public BookmarksRepositorySession getSession() {
//    return session;
//  }
//
//  public void setSession(BookmarksRepositorySession session) {
//    this.session = session;
//  }
//
//}
