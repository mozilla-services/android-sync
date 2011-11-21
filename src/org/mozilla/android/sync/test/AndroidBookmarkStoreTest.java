/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.MainActivity;
import org.mozilla.android.sync.repositories.bookmarks.BookmarksDatabaseHelper;
import org.mozilla.android.sync.repositories.bookmarks.BookmarksRepositorySession;
import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.test.helpers.BookmarkHelpers;
import org.mozilla.android.sync.test.helpers.ExpectFetchAllDelegate;
import org.mozilla.android.sync.test.helpers.ExpectStoredDelegate;

import android.content.Context;
import android.test.ActivityInstrumentationTestCase2;

public class AndroidBookmarkStoreTest extends ActivityInstrumentationTestCase2<MainActivity> {
  public AndroidBookmarkStoreTest() {
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

  private void prepSession() {
    AndroidBookmarksTestHelper.prepareRepositorySession(getApplicationContext(), new SetupDelegate(), 0);
    // Clear old data.
    new BookmarksDatabaseHelper(getApplicationContext()).wipe();
  }

  public void tearDown() {
    new BookmarksDatabaseHelper(getApplicationContext()).close();
  }

  /*
   * This test class is dedicated to testing the store(record) method of
   * RepositorySession.
   */

  /*
   * Test storing each different type of Bookmark record.
   */

  public void testStoreBookmark() {
    prepSession();
    BookmarkRecord bookmark = BookmarkHelpers.createBookmark1();
    getSession().store(bookmark, new ExpectStoredDelegate(bookmark.guid));
    performWait();
  }

  public void testStoreMicrosummary() {
    prepSession();
    BookmarkRecord bookmark = BookmarkHelpers.createMicrosummary();
    getSession().store(bookmark, new ExpectStoredDelegate(bookmark.guid));
    performWait();
  }

  public void testStoreQuery() {
    prepSession();
    BookmarkRecord bookmark = BookmarkHelpers.createQuery();
    getSession().store(bookmark, new ExpectStoredDelegate(bookmark.guid));
    performWait();
  }

  public void testStoreFolder() {
    prepSession();
    BookmarkRecord bookmark = BookmarkHelpers.createFolder();
    getSession().store(bookmark, new ExpectStoredDelegate(bookmark.guid));
    performWait();
  }

  public void testStoreLivemark() {
    prepSession();
    BookmarkRecord bookmark = BookmarkHelpers.createLivemark();
    getSession().store(bookmark, new ExpectStoredDelegate(bookmark.guid));
    performWait();
  }

  public void testStoreSeparator() {
    prepSession();
    BookmarkRecord bookmark = BookmarkHelpers.createSeparator();
    getSession().store(bookmark, new ExpectStoredDelegate(bookmark.guid));
    performWait();
  }

  /*
   * Tests for resolving conflicts where a record with the given GUID already
   * exists.
   */

  /*
   * The most basic test.
   *
   * Record being stored has newer timestamp than existing local record, local
   * record has not been modified since last sync.
   */

  public void testRemoteNewerTimeStamp() {
    prepSession();

    // Record existing and hasn't changed since before lastSync.
    // Automatically will be assigned lastModified = current time.
    BookmarkRecord local = BookmarkHelpers.createBookmark1();
    local.androidID = 54321;
    getSession().store(local, new ExpectStoredDelegate(local.guid));
    performWait();

    // Create second bookmark to be passed to store. Give it a later
    // last modified timestamp and set it as same GUID.
    BookmarkRecord remote = BookmarkHelpers.createBookmark2();
    remote.guid = local.guid;
    remote.lastModified = local.lastModified + 1000;
    getSession().store(remote, new ExpectStoredDelegate(remote.guid));
    performWait();

    String[] expected = new String[] { local.guid };
    ExpectFetchAllDelegate delegate = new ExpectFetchAllDelegate(expected);
    getSession().fetchAll(delegate);
    performWait();

    // Check that one record comes back, it is the remote one, and has android
    // ID same as first.
    assertEquals(1, delegate.records.length);
    BookmarkRecord record = (BookmarkRecord) delegate.records[0];
    BookmarkHelpers.verifyExpectedRecordReturned(remote, record);
    assertEquals(local.androidID, record.androidID);
  }

  /*
   * Local record has a newer timestamp than the record being stored. For now,
   * we just take newer (local) record)
   */

  public void testLocalNewerTimeStamp() {
    prepSession();

    // Local record newer.
    long timestamp = 1000000000;
    BookmarkRecord local = BookmarkHelpers.createBookmark1();
    local.androidID = 54321;
    local.lastModified = timestamp;
    getSession().store(local, new ExpectStoredDelegate(local.guid));
    performWait();

    // Create an older version of a record with the same GUID.
    BookmarkRecord remote = BookmarkHelpers.createBookmark2();
    remote.guid = local.guid;
    remote.lastModified = timestamp - 100;
    getSession().store(remote, new ExpectStoredDelegate(remote.guid));
    performWait();

    // Do a fetch and make sure that we get back the first (local) record.
    String[] expected = new String[] { local.guid };
    ExpectFetchAllDelegate delegate = new ExpectFetchAllDelegate(expected);
    getSession().fetchAll(delegate);
    performWait();

    // Check that one record comes back, it is the local one
    assertEquals(1, delegate.recordCount());
    BookmarkRecord record = (BookmarkRecord) (delegate.records[0]);
    BookmarkHelpers.verifyExpectedRecordReturned(local, record);
    assertEquals(local.androidID, record.androidID);
  }
}
