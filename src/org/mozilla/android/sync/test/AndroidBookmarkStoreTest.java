/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.test.helpers.BookmarkHelpers;
import org.mozilla.android.sync.test.helpers.DefaultStoreDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFetchAllDelegate;
import org.mozilla.android.sync.test.helpers.ExpectStoredDelegate;

public class AndroidBookmarkStoreTest extends AndroidBookmarksTestBase {

  /*
   * This test class is dedicated to testing the store(record) method of
   * RepositorySession.
   */

  /*
   * Test storing each different type of Bookmark record.
   */

  public void testStoreBookmark() {
    basicStoreTest(BookmarkHelpers.createBookmark1());
  }

  public void testStoreMicrosummary() {
    basicStoreTest(BookmarkHelpers.createMicrosummary());
  }


  public void testStoreQuery() {
    basicStoreTest(BookmarkHelpers.createQuery());
  }

  public void testStoreFolder() {
    basicStoreTest(BookmarkHelpers.createFolder());
  }

  public void testStoreLivemark() {
    basicStoreTest(BookmarkHelpers.createLivemark());
  }

  public void testStoreSeparator() {
    basicStoreTest(BookmarkHelpers.createSeparator());
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
    Runnable localRunnable = getStoreRunnable(local, new ExpectStoredDelegate(local.guid));
    performWait(localRunnable);

    // Create second bookmark to be passed to store. Give it a later
    // last modified timestamp and set it as same GUID.
    BookmarkRecord remote = BookmarkHelpers.createBookmark2();
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
    Runnable localRunnable = getStoreRunnable(local, new ExpectStoredDelegate(local.guid));
    performWait(localRunnable);

    // Create an older version of a record with the same GUID.
    BookmarkRecord remote = BookmarkHelpers.createBookmark2();
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
    BookmarkRecord record = (BookmarkRecord) (delegate.records[0]);
    BookmarkHelpers.verifyExpectedRecordReturned(local, record);
    assertEquals(local.androidID, record.androidID);
  }
  
  /*
   * Insert a record that is marked as deleted, remote has newer timestamp
   */
  
  public void testDeleteRemoteNewer() {
    prepSession();
    
    // Record existing and hasn't changed since before lastSync.
    // Automatically will be assigned lastModified = current time.
    BookmarkRecord local = BookmarkHelpers.createBookmark1();
    local.androidID = 54321;
    Runnable local1Runnable = getStoreRunnable(local, new ExpectStoredDelegate(local.guid));
    performWait(local1Runnable);

    // Pass the same record to store, but mark it deleted and modified
    // more recently
    local.lastModified = local.lastModified + 1000;
    local.deleted = true;
    local.androidID = 0;
    Runnable local2Runnable = getStoreRunnable(local, new ExpectStoredDelegate(local.guid));
    performWait(local2Runnable);

    String[] expected = new String[] { local.guid };
    ExpectFetchAllDelegate delegate = new ExpectFetchAllDelegate(expected);
    performWait(getFetchAllRunnable(delegate));

    // Check that one record comes back, marked deleted and with
    // and androidId
    assertEquals(1, delegate.records.length);
    BookmarkRecord record = (BookmarkRecord) delegate.records[0];
    local.androidID = 54321;
    BookmarkHelpers.verifyExpectedRecordReturned(local, record);
    assertEquals(local.androidID, record.androidID);
    assertEquals(true, record.deleted);
    
  }
  
  /*
   * Insert a record that is marked as deleted, local has newer timestamp
   * and was not marked deleted (so keep it)
   */
  public void testDeleteLocalNewer() {
    prepSession();

    // Local record newer.
    long timestamp = 1000000000;
    BookmarkRecord local = BookmarkHelpers.createBookmark1();
    local.androidID = 54321;
    local.lastModified = timestamp;
    Runnable localRunnable = getStoreRunnable(local, new ExpectStoredDelegate(local.guid));
    performWait(localRunnable);

    // Create an older version of a record with the same GUID.
    BookmarkRecord remote = BookmarkHelpers.createBookmark1();
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
    BookmarkRecord record = (BookmarkRecord) (delegate.records[0]);
    BookmarkHelpers.verifyExpectedRecordReturned(local, record);
    assertEquals(local.androidID, record.androidID);
    assertEquals(false, record.deleted);
  }
  
  /*
   * Insert a record that is marked as deleted, record never existed locally
   */
  public void testDeleteRemoteLocalNonexistent() {
    prepSession();
    
    long timestamp = 1000000000;
    
    // Pass a record marked deleted to store, doesn't exist locally
    BookmarkRecord remote = BookmarkHelpers.createBookmark1(); 
    remote.lastModified = timestamp;
    remote.deleted = true;
    Runnable remoteRunnable = getStoreRunnable(remote, new ExpectStoredDelegate(remote.guid));
    performWait(remoteRunnable);

    ExpectFetchAllDelegate delegate = new ExpectFetchAllDelegate(new String[]{});
    performWait(getFetchAllRunnable(delegate));

    // Check that no records are returned
    assertEquals(0, delegate.records.length);
  }
}
