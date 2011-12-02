/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.repositories.android.AndroidBrowserBookmarksDatabaseHelper;
import org.mozilla.android.sync.repositories.android.AndroidBrowserBookmarksRepository;
import org.mozilla.android.sync.repositories.android.AndroidBrowserRepository;
import org.mozilla.android.sync.repositories.android.AndroidBrowserRepositoryDatabaseHelper;
import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.repositories.domain.Record;
import org.mozilla.android.sync.test.helpers.BookmarkHelpers;

public class AndroidBookmarksRepoTest extends AndroidRepositoryTest {
  
  @Override
  protected AndroidBrowserRepository getRepository() {
    return new AndroidBrowserBookmarksRepository();
  }
  
  @Override
  protected AndroidBrowserRepositoryDatabaseHelper getDatabaseHelper() {
    return new AndroidBrowserBookmarksDatabaseHelper(getApplicationContext());
  }
 
  public void testFetchAll() {
    Record[] expected = new Record[2];
    expected[0] = BookmarkHelpers.createBookmark1();
    expected[1] = BookmarkHelpers.createBookmark2();
    basicFetchAllTest(expected);
  }

  /*
   * Tests for fetching GUIDs since a timestamp.
   */
  public void testGuidsSinceReturnMultipleRecords() {
    BookmarkRecord record0 = BookmarkHelpers.createLivemark();
    BookmarkRecord record1 = BookmarkHelpers.createMicrosummary();
    guidsSinceReturnMultipleRecords(record0, record1);
  }
  
  public void testGuidsSinceReturnNoRecords() {
    guidsSinceReturnNoRecords(BookmarkHelpers.createLivemark());
  }

  /*
   * Tests for fetchSince
   */  
  public void testFetchSinceOneRecord() {
    fetchSinceOneRecord(BookmarkHelpers.createFolder(),
        BookmarkHelpers.createBookmark2());
  }

  public void testFetchSinceReturnNoRecords() {
    fetchSinceReturnNoRecords(BookmarkHelpers.createBookmark1());
  }

  /*
   * Tests for fetch(guid)
   */
  
  public void testFetchOneRecordByGuid() {
    fetchOneRecordByGuid(BookmarkHelpers.createBookmark1(),
        BookmarkHelpers.createBookmark2());
  }
  
  public void testFetchMultipleRecordsByGuids() {
    BookmarkRecord record0 = BookmarkHelpers.createBookmark1();
    BookmarkRecord record1 = BookmarkHelpers.createBookmark2();
    BookmarkRecord record2 = BookmarkHelpers.createQuery();
    fetchMultipleRecordsByGuids(record0, record1, record2);
  }
  
  public void testFetchNoRecordByGuid() {
    fetchNoRecordByGuid(BookmarkHelpers.createSeparator());
  }
  
    
  /*
   * Test wipe
   */
  public void testWipe() {
    doWipe(BookmarkHelpers.createFolder(), BookmarkHelpers.createBookmark2());
  }
  
  /*
   * Test store
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
  
  /*
   * Test for store conflict resolution
   * NOTE: Must set an android ID for local record for these tests to work
   */
  public void testRemoteNewerTimeStamp() {
    BookmarkRecord local = BookmarkHelpers.createBookmark1();
    BookmarkRecord remote = BookmarkHelpers.createBookmark2();
    local.androidID = 54321;
    remoteNewerTimeStamp(local, remote);
  }

  public void testLocalNewerTimeStamp() {
    BookmarkRecord local = BookmarkHelpers.createBookmark1();
    BookmarkRecord remote = BookmarkHelpers.createBookmark2();
    local.androidID = 54321;
    localNewerTimeStamp(local, remote);
  }
  
  public void testDeleteRemoteNewer() {
    BookmarkRecord local = BookmarkHelpers.createBookmark1();
    BookmarkRecord remote = BookmarkHelpers.createBookmark2();
    local.androidID = 54321;
    deleteRemoteNewer(local, remote);
  }
  
  public void testDeleteLocalNewere() {
    BookmarkRecord local = BookmarkHelpers.createBookmark1();
    BookmarkRecord remote = BookmarkHelpers.createBookmark2();
    local.androidID = 54321;
    deleteLocalNewer(local, remote);
  }
  
  public void testDeleteRemoteLocalNonexistent() {
    BookmarkRecord remote = BookmarkHelpers.createBookmark2();
    deleteRemoteLocalNonexistent(remote);
  }
  
  /*
   * Helpers
   */
  @Override
  protected void verifyExpectedRecordReturned(Record expected, Record actual) {
    BookmarkRecord recExpect = (BookmarkRecord) expected;
    BookmarkRecord recActual = (BookmarkRecord) actual;
    BookmarkHelpers.verifyExpectedRecordReturned(recExpect, recActual);
    assertEquals(recExpect.androidID, recActual.androidID);
  }

}