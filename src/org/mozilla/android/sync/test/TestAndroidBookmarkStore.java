/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import static junit.framework.Assert.assertEquals;

import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.test.helpers.BookmarkHelpers;
import org.mozilla.android.sync.test.helpers.ExpectFetchAllDelegate;
import org.mozilla.android.sync.test.helpers.ExpectStoredDelegate;

public class TestAndroidBookmarkStore extends AndroidBookmarksTestHelper {
  /*
   * This test class is dedicated to testing the
   * store(record) method of RepositorySession.
   */

  /*
   * Test storing each different type of Bookmark record.
   */
  
  public void testStoreBookmark() {
    this.prepareRepositorySession(new SetupDelegate(), 0);
    BookmarkRecord bookmark = BookmarkHelpers.createBookmark1();
    session.store(bookmark, new ExpectStoredDelegate(bookmark.guid));
    testWaiter.performWait();
  }

  
  public void testStoreMicrosummary() {
    this.prepareRepositorySession(new SetupDelegate(), 0);
    BookmarkRecord bookmark = BookmarkHelpers.createMicrosummary();
    session.store(bookmark, new ExpectStoredDelegate(bookmark.guid));
    testWaiter.performWait();
  }

  
  public void testStoreQuery() {
    this.prepareRepositorySession(new SetupDelegate(), 0);
    BookmarkRecord bookmark = BookmarkHelpers.createQuery();
    session.store(bookmark, new ExpectStoredDelegate(bookmark.guid));
    testWaiter.performWait();
  }

  
  public void testStoreFolder() {
    this.prepareRepositorySession(new SetupDelegate(), 0);
    BookmarkRecord bookmark = BookmarkHelpers.createFolder();
    session.store(bookmark, new ExpectStoredDelegate(bookmark.guid));
    testWaiter.performWait();
  }

  
  public void testStoreLivemark() {
    this.prepareRepositorySession(new SetupDelegate(), 0);
    BookmarkRecord bookmark = BookmarkHelpers.createLivemark();
    session.store(bookmark, new ExpectStoredDelegate(bookmark.guid));
    testWaiter.performWait();
  }

  
  public void testStoreSeparator() {
    this.prepareRepositorySession(new SetupDelegate(), 0);
    BookmarkRecord bookmark = BookmarkHelpers.createSeparator();
    session.store(bookmark, new ExpectStoredDelegate(bookmark.guid));
    testWaiter.performWait();
  }

  /*
   * Tests for resolving conflicts where a record with the given GUID already exists.
   */

  /*
   * The most basic test.
   *
   * Record being stored has newer timestamp than existing local record, local record has not been modified since last sync.
   */
  
  public void testRemoteNewerTimeStamp() {
    // Record existing and hasn't changed since before lastSync.
    // Automatically will be assigned lastModified = current time.
    BookmarkRecord local = BookmarkHelpers.createBookmark1();
    local.androidID = 54321;
    session.store(local, new ExpectStoredDelegate(local.guid));
    testWaiter.performWait();

    // Create second bookmark to be passed to store. Give it a later
    // last modified timestamp and set it as same guid.
    BookmarkRecord remote = BookmarkHelpers.createBookmark2();
    remote.guid = local.guid;
    remote.lastModified = local.lastModified + 1000;
    session.store(remote, new ExpectStoredDelegate(remote.guid));
    testWaiter.performWait();

    String[] expected = new String[] { local.guid };
    ExpectFetchAllDelegate delegate = new ExpectFetchAllDelegate(expected);
    session.fetchAll(delegate);
    testWaiter.performWait();

    // Check that one record comes back, it is the remote one, and has android ID same as first.
    assertEquals(1, delegate.records.length);
    BookmarkRecord record = (BookmarkRecord) delegate.records[0];
    BookmarkHelpers.verifyExpectedRecordReturned(remote, record);
    assertEquals(local.androidID, record.androidID);
  }

  /*
   * Local record has a newer timestamp than
   * record being stored. For now, we just take newer (local) record)
   */
//  
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
}
