/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.test.helpers.BookmarkHelpers;
import org.mozilla.android.sync.test.helpers.DefaultFinishDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFetchDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFinishDelegate;
import org.mozilla.android.sync.test.helpers.ExpectInvalidTypeStoreDelegate;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.BookmarkNeedsReparentingException;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserBookmarksDataAccessor;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserBookmarksRepository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepositoryDataAccessor;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepositorySession;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.util.Log;

public class AndroidBrowserBookmarksRepositoryTest extends AndroidBrowserRepositoryTest {
  
  @Override
  protected AndroidBrowserRepository getRepository() {
    return new AndroidBrowserBookmarksRepository();
  }
  
  @Override
  protected AndroidBrowserRepositoryDataAccessor getDataAccessor() {
    return new AndroidBrowserBookmarksDataAccessor(getApplicationContext());
  }
 
  // NOTE NOTE NOTE
  // Currently queries require the qualifier (i.e. "bookmarks.guid"), inserts
  // require that the qualifier is not there. Oh...and the qualifier is only
  // required for columns that exist in multiple tables (like guid, url, etc.)
  // UGLY, MESSY, and with some luck and cooperation from #mobile TEMPORARY!
  
  // ALSO must store folder before records if we we are checking that the
  // records returned are the same as those sent in. Parent folder resolution
  // not quite right yet, partially because missing special folders in conent
  // provider. Change this back later since we want to test not doing this.
  
  @Override
  public void testFetchAll() {
    Record[] expected = new Record[3];
    expected[0] = BookmarkHelpers.createFolder1();
    expected[1] = BookmarkHelpers.createBookmark1();
    expected[2] = BookmarkHelpers.createBookmark2();
    basicFetchAllTest(expected);
  }
  
  @Override
  public void testGuidsSinceReturnMultipleRecords() {
    BookmarkRecord record0 = BookmarkHelpers.createBookmark1();
    BookmarkRecord record1 = BookmarkHelpers.createBookmark2();
    guidsSinceReturnMultipleRecords(record0, record1);
  }
  
  /*
   *TODO anything "since" fails due to content provider overwriting our
   *last modified time stamps. Need to find a new way to test this.
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
  */

  @Override
  public void testFetchOneRecordByGuid() {
    fetchOneRecordByGuid(BookmarkHelpers.createFolder1(),
        BookmarkHelpers.createBookmark2());
  }
  
  @Override
  public void testFetchMultipleRecordsByGuids() {
    BookmarkRecord record0 = BookmarkHelpers.createFolder1();
    BookmarkRecord record1 = BookmarkHelpers.createBookmark1();
    BookmarkRecord record2 = BookmarkHelpers.createBookmark2();
    fetchMultipleRecordsByGuids(record0, record1, record2);
  }
  
  @Override
  public void testFetchNoRecordByGuid() {
    fetchNoRecordByGuid(BookmarkHelpers.createBookmark1());
  }
  
    
  @Override
  public void testWipe() {
    doWipe(BookmarkHelpers.createFolder1(), BookmarkHelpers.createBookmark2());
  }
  
  @Override
  public void testStore() {
    basicStoreTest(BookmarkHelpers.createBookmark1());
  }

  /*
  @Override
  public void testRemoteNewerTimeStamp() {
    // TODO Auto-generated method stub
    Log.w(tag, "This test didn't actually pass. It is currently just a stub. " +
    		"Timing event related tests need to be modified since Fennec content " +
    		"providers overwrite our faked lastModified times (this is the correct " +
    		"action for Fennec, but doesn't let us test the way we were).");
  }
  */

  /*
  @Override
  public void testLocalNewerTimeStamp() {
    // TODO Auto-generated method stub
    Log.w(tag, "This test didn't actually pass. It is currently just a stub. " +
    		"Timing event related tests need to be modified since Fennec content " +
    		"providers overwrite our faked lastModified times (this is the correct " +
    		"action for Fennec, but doesn't let us test the way we were).");
  }
  */

  /*
  @Override
  public void testDeleteRemoteNewer() {
    // TODO Auto-generated method stub
    Log.w(tag, "This test didn't actually pass. It is currently just a stub. " +
    		"Timing event related tests need to be modified since Fennec content " +
    		"providers overwrite our faked lastModified times (this is the correct " +
    		"action for Fennec, but doesn't let us test the way we were).");
  }
  */

  /*
  @Override
  public void testDeleteLocalNewer() {
    // TODO Auto-generated method stub
    Log.w(tag, "This test didn't actually pass. It is currently just a stub. " +
    		"Timing event related tests need to be modified since Fennec content " +
    		"providers overwrite our faked lastModified times (this is the correct " +
    		"action for Fennec, but doesn't let us test the way we were).");
  }*/

  /*
  @Override
  public void testDeleteRemoteLocalNonexistent() {
    // TODO Auto-generated method stub
    Log.w(tag, "This test didn't actually pass. It is currently just a stub. " +
    		"Timing event related tests need to be modified since Fennec content " +
    		"providers overwrite our faked lastModified times (this is the correct " +
    		"action for Fennec, but doesn't let us test the way we were).");
  }
  */

  @Override
  public void testFetchSinceOneRecord() {
    // TODO Auto-generated method stub
    Log.w(tag, "This test didn't actually pass. It is currently just a stub. " +
    		"Timing event related tests need to be modified since Fennec content " +
    		"providers overwrite our faked lastModified times (this is the correct " +
    		"action for Fennec, but doesn't let us test the way we were).");
  }

  @Override
  public void testFetchSinceReturnNoRecords() {
    // TODO Auto-generated method stub
    Log.w(tag, "This test didn't actually pass. It is currently just a stub. " +
    		"Timing event related tests need to be modified since Fennec content " +
    		"providers overwrite our faked lastModified times (this is the correct " +
    		"action for Fennec, but doesn't let us test the way we were).");
  }

  @Override
  public void testGuidsSinceReturnNoRecords() {
    // TODO Auto-generated method stub
    Log.w(tag, "This test didn't actually pass. It is currently just a stub. " +
    		"Timing event related tests need to be modified since Fennec content " +
    		"providers overwrite our faked lastModified times (this is the correct " +
    		"action for Fennec, but doesn't let us test the way we were).");
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
    basicStoreTest(BookmarkHelpers.createFolder1());
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
  
  /*
   * Re-parenting tests
   */
  // Insert two records missing parent, then insert their parent.
  // Make sure they end up with the correct parent on fetch.
  public void testBasicReparenting() {
    Record[] expected = new Record[] {
        BookmarkHelpers.createBookmark1(),
        BookmarkHelpers.createBookmark2(),
        BookmarkHelpers.createFolder1()
    };
    doMultipleFolderReparentingTest(expected);
  }
  
  // Insert 3 folders and 4 bookmarks in different orders
  // and make sure they come out parented correctly
  public void testMultipleFolderReparenting1() {
    Record[] expected = new Record[] {
        BookmarkHelpers.createBookmark1(),
        BookmarkHelpers.createBookmark2(),
        BookmarkHelpers.createBookmark3(),
        BookmarkHelpers.createFolder1(),
        BookmarkHelpers.createBookmark4(),
        BookmarkHelpers.createFolder3(),
        BookmarkHelpers.createFolder2(),
    };
    doMultipleFolderReparentingTest(expected);
  }
  
  public void testMultipleFolderReparenting2() {
    Record[] expected = new Record[] {
        BookmarkHelpers.createBookmark1(),
        BookmarkHelpers.createBookmark2(),
        BookmarkHelpers.createBookmark3(),
        BookmarkHelpers.createFolder1(),
        BookmarkHelpers.createBookmark4(),
        BookmarkHelpers.createFolder3(),
        BookmarkHelpers.createFolder2(),
    };
    doMultipleFolderReparentingTest(expected);
  }
  
  public void testMultipleFolderReparenting3() {
    Record[] expected = new Record[] {
        BookmarkHelpers.createBookmark1(),
        BookmarkHelpers.createBookmark2(),
        BookmarkHelpers.createBookmark3(),
        BookmarkHelpers.createFolder1(),
        BookmarkHelpers.createBookmark4(),
        BookmarkHelpers.createFolder3(),
        BookmarkHelpers.createFolder2(),
    };
    doMultipleFolderReparentingTest(expected);
  }
  
  private void doMultipleFolderReparentingTest(Record[] expected) {
    prepSession();
    AndroidBrowserRepositorySession session = getSession();
    doStore(session, expected);
    ExpectFetchDelegate delegate = new ExpectFetchDelegate(expected);
    performWait(fetchAllRunnable(session, delegate));
    session.finish(new ExpectFinishDelegate());
  }
  
  
  // Insert a record without a parent and check that it is
  // put into unfiled bookmarks. Call finish() and check
  // for an error returned stating that there are still
  // records that need to be re-parented.
  public void testFinishBeforeReparent() {
    prepSession();
    AndroidBrowserRepositorySession session = getSession();
    Record[] records = new Record[] {
      BookmarkHelpers.createBookmark1()  
    };
    doStore(session, records);
    session.finish(new DefaultFinishDelegate() {
      @Override
      public void onFinishFailed(Exception ex) {
        if (ex.getClass() != BookmarkNeedsReparentingException.class) {
          fail("Expected: " + BookmarkNeedsReparentingException.class + " but got " + ex.getClass());
        }
      }
    });
    
    
  }
  
  /*
   * Test storing identical records with different guids.
   * For bookmarks identical is defined by the following fields
   * being the same: title, uri, type, parentName
   */
  public void testStoreIdenticalExceptGuid() {
    Record record0 = BookmarkHelpers.createBookmarkInMobileFolder1();
    Record record1 = BookmarkHelpers.createBookmarkInMobileFolder1();
    record1.guid = Utils.generateGuid();
    assert(!record0.guid.equals(record1.guid));
    storeIdenticalExceptGuid(record0, record1);
  }
  
  // More complicated situation in which we insert folders
  // that exist with children but with a different guid.
  // TODO basic tests for inserting existing records with
  // different guid first.
  
  @Override
  public void testRemoteNewerTimeStamp() {
    BookmarkRecord local = BookmarkHelpers.createBookmarkInMobileFolder1();
    BookmarkRecord remote = BookmarkHelpers.createBookmarkInMobileFolder2();
    remoteNewerTimeStamp(local, remote);
  }

  @Override
  public void testLocalNewerTimeStamp() {
    BookmarkRecord local = BookmarkHelpers.createBookmarkInMobileFolder1();
    BookmarkRecord remote = BookmarkHelpers.createBookmarkInMobileFolder2();
    localNewerTimeStamp(local, remote);
  }
  
  @Override
  public void testDeleteRemoteNewer() {
    BookmarkRecord local = BookmarkHelpers.createBookmarkInMobileFolder1();
    BookmarkRecord remote = BookmarkHelpers.createBookmarkInMobileFolder2();
    deleteRemoteNewer(local, remote);
  }
  
  @Override
  public void testDeleteLocalNewer() {
    BookmarkRecord local = BookmarkHelpers.createBookmarkInMobileFolder1();
    BookmarkRecord remote = BookmarkHelpers.createBookmarkInMobileFolder2();
    deleteLocalNewer(local, remote);
  }
  
  @Override
  public void testDeleteRemoteLocalNonexistent() {
    BookmarkRecord remote = BookmarkHelpers.createBookmark2();
    deleteRemoteLocalNonexistent(remote);
  }
}
