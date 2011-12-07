/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.repositories.android.AndroidBrowserBookmarksDataAccessor;
import org.mozilla.android.sync.repositories.android.AndroidBrowserBookmarksRepository;
import org.mozilla.android.sync.repositories.android.AndroidBrowserRepository;
import org.mozilla.android.sync.repositories.android.AndroidBrowserRepositoryDataAccessor;
import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.repositories.domain.Record;
import org.mozilla.android.sync.test.helpers.BookmarkHelpers;
import org.mozilla.android.sync.test.helpers.ExpectInvalidTypeStoreDelegate;

public class AndroidBrowserBookmarksRepositoryTest extends AndroidBrowserRepositoryTest {
  
  @Override
  protected AndroidBrowserRepository getRepository() {
    return new AndroidBrowserBookmarksRepository();
  }
  
  @Override
  protected AndroidBrowserRepositoryDataAccessor getDataAccessor() {
    return new AndroidBrowserBookmarksDataAccessor(getApplicationContext());
  }
 
  @Override
  public void testFetchAll() {
    Record[] expected = new Record[2];
    expected[0] = BookmarkHelpers.createBookmark1();
    expected[1] = BookmarkHelpers.createBookmark2();
    basicFetchAllTest(expected);
  }

  @Override
  public void testGuidsSinceReturnMultipleRecords() {
    BookmarkRecord record0 = BookmarkHelpers.createBookmark1();
    BookmarkRecord record1 = BookmarkHelpers.createBookmark2();
    guidsSinceReturnMultipleRecords(record0, record1);
  }
  
  @Override
  public void testGuidsSinceReturnNoRecords() {
    guidsSinceReturnNoRecords(BookmarkHelpers.createBookmark1());
  }

  @Override
  public void testFetchSinceOneRecord() {
    fetchSinceOneRecord(BookmarkHelpers.createFolder(),
        BookmarkHelpers.createBookmark2());
  }

  @Override
  public void testFetchSinceReturnNoRecords() {
    fetchSinceReturnNoRecords(BookmarkHelpers.createBookmark1());
  }

  @Override
  public void testFetchOneRecordByGuid() {
    fetchOneRecordByGuid(BookmarkHelpers.createBookmark1(),
        BookmarkHelpers.createBookmark2());
  }
  
  @Override
  public void testFetchMultipleRecordsByGuids() {
    BookmarkRecord record0 = BookmarkHelpers.createBookmark1();
    BookmarkRecord record1 = BookmarkHelpers.createBookmark2();
    BookmarkRecord record2 = BookmarkHelpers.createFolder();
    fetchMultipleRecordsByGuids(record0, record1, record2);
  }
  
  @Override
  public void testFetchNoRecordByGuid() {
    fetchNoRecordByGuid(BookmarkHelpers.createBookmark1());
  }
  
    
  @Override
  public void testWipe() {
    doWipe(BookmarkHelpers.createFolder(), BookmarkHelpers.createBookmark2());
  }
  
  @Override
  public void testStore() {
    basicStoreTest(BookmarkHelpers.createBookmark1());
  }

  /*
   * Test storing each different type of Bookmark record.
   * TODO We expect any records with type other than "bookmark"
   * or "folder" to fail. For now we throw these away.
   */
  public void testStoreMicrosummary() {
    basicStoreFailTest(BookmarkHelpers.createMicrosummary());
  }

  public void testStoreQuery() {
    basicStoreFailTest(BookmarkHelpers.createQuery());
  }

  public void testStoreFolder() {
    basicStoreTest(BookmarkHelpers.createFolder());
  }

  public void testStoreLivemark() {
    basicStoreFailTest(BookmarkHelpers.createLivemark());
  }

  public void testStoreSeparator() {
    basicStoreFailTest(BookmarkHelpers.createSeparator());
  }
  
  protected void basicStoreFailTest(Record record) {
    prepSession();    
    performWait(storeRunnable(getSession(), record, new ExpectInvalidTypeStoreDelegate()));
  }
  
  @Override
  public void testRemoteNewerTimeStamp() {
    BookmarkRecord local = BookmarkHelpers.createBookmark1();
    BookmarkRecord remote = BookmarkHelpers.createBookmark2();
    local.androidID = 54321;
    remoteNewerTimeStamp(local, remote);
  }

  @Override
  public void testLocalNewerTimeStamp() {
    BookmarkRecord local = BookmarkHelpers.createBookmark1();
    BookmarkRecord remote = BookmarkHelpers.createBookmark2();
    local.androidID = 54321;
    localNewerTimeStamp(local, remote);
  }
  
  @Override
  public void testDeleteRemoteNewer() {
    BookmarkRecord local = BookmarkHelpers.createBookmark1();
    BookmarkRecord remote = BookmarkHelpers.createBookmark2();
    local.androidID = 54321;
    deleteRemoteNewer(local, remote);
  }
  
  @Override
  public void testDeleteLocalNewer() {
    BookmarkRecord local = BookmarkHelpers.createBookmark1();
    BookmarkRecord remote = BookmarkHelpers.createBookmark2();
    local.androidID = 54321;
    deleteLocalNewer(local, remote);
  }
  
  @Override
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