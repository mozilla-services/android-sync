/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.json.simple.JSONArray;
import org.mozilla.android.sync.test.helpers.BookmarkHelpers;
import org.mozilla.android.sync.test.helpers.DefaultFinishDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFetchDelegate;
import org.mozilla.android.sync.test.helpers.ExpectFinishDelegate;
import org.mozilla.android.sync.test.helpers.ExpectInvalidTypeStoreDelegate;
import org.mozilla.gecko.sync.Utils;
import org.mozilla.gecko.sync.repositories.BookmarkNeedsReparentingException;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserBookmarksDataAccessor;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserBookmarksRepository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepository;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepositoryDataAccessor;
import org.mozilla.gecko.sync.repositories.android.AndroidBrowserRepositorySession;
import org.mozilla.gecko.sync.repositories.android.BrowserContract;
import org.mozilla.gecko.sync.repositories.android.RepoUtils;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
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
  // Must store folder before records if we we are checking that the
  // records returned are the same as those sent in. If the folder isn't stored
  // first, the returned records won't be identical to those stored because we
  // aren't able to find the parent name/guid when we do a fetch. If you don't want
  // to store a folder first, store your record in "mobile" or one of the folders
  // that always exists.

  public void testFetchOneWithChildren() {
    BookmarkRecord folder = BookmarkHelpers.createFolder1();
    BookmarkRecord bookmark1 = BookmarkHelpers.createBookmark1();
    BookmarkRecord bookmark2 = BookmarkHelpers.createBookmark2();

    AndroidBrowserRepositoryTestHelper.prepareRepositorySession(getApplicationContext(),
        new SetupDelegate(), true, getRepository());
    Log.i("rnewman", "Prepared.");

    AndroidBrowserRepositorySession session = getSession();
    BookmarkHelpers.dumpBookmarksDB(getApplicationContext());
    performWait(storeRunnable(session, folder));
    performWait(storeRunnable(session, bookmark1));
    performWait(storeRunnable(session, bookmark2));
    BookmarkHelpers.dumpBookmarksDB(getApplicationContext());
    performWait(fetchRunnable(session, new String[] { folder.guid }, new Record[] { folder }));

  }

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
  
  @Override
  public void testGuidsSinceReturnNoRecords() {
    guidsSinceReturnNoRecords(BookmarkHelpers.createBookmarkInMobileFolder1());
  }

  @Override
  public void testFetchSinceOneRecord() {
    fetchSinceOneRecord(BookmarkHelpers.createBookmarkInMobileFolder1(),
        BookmarkHelpers.createBookmarkInMobileFolder2());
  }

  @Override
  public void testFetchSinceReturnNoRecords() {
    fetchSinceReturnNoRecords(BookmarkHelpers.createBookmark1());
  }

  @Override
  public void testFetchOneRecordByGuid() {
    fetchOneRecordByGuid(BookmarkHelpers.createBookmarkInMobileFolder1(),
        BookmarkHelpers.createBookmarkInMobileFolder2());
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
    doWipe(BookmarkHelpers.createBookmarkInMobileFolder1(), BookmarkHelpers.createBookmarkInMobileFolder2());
  }
  
  @Override
  public void testStore() {
    basicStoreTest(BookmarkHelpers.createBookmark1());
  }


  public void testStoreFolder() {
    basicStoreTest(BookmarkHelpers.createFolder1());
  }

  /**
   * TODO: 2011-12-24, tests disabled because we no longer fail
   * a store call if we get an unknown record type.
   */
  /*
   * Test storing each different type of Bookmark record.
   * We expect any records with type other than "bookmark"
   * or "folder" to fail. For now we throw these away.
   */
  /*
  public void testStoreMicrosummary() {
    basicStoreFailTest(BookmarkHelpers.createMicrosummary());
  }

  public void testStoreQuery() {
    basicStoreFailTest(BookmarkHelpers.createQuery());
  }

  public void testStoreLivemark() {
    basicStoreFailTest(BookmarkHelpers.createLivemark());
  }

  public void testStoreSeparator() {
    basicStoreFailTest(BookmarkHelpers.createSeparator());
  }
  */
  
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
  @Override
  public void testStoreIdenticalExceptGuid() {
    Log.i("testStoreIdenticalExceptGuid", "Started.");
    storeIdenticalExceptGuid(BookmarkHelpers.createBookmarkInMobileFolder1());
    Log.i("testStoreIdenticalExceptGuid", "Done.");
  }
  
  /*
   * More complicated situation in which we insert a folder
   * followed by a couple of its children. We then insert
   * the folder again but with a different guid. Children
   * must still get correct parent when they are fetched.
   * Store a record after with the new guid as the parent
   * and make sure it works as well.
   */
  public void testStoreIdenticalFoldersWithChildren() {
    prepSession();
    AndroidBrowserRepositorySession session = getSession();
    Record record0 = BookmarkHelpers.createFolder1();

    // Get timestamp so that the conflicting folder that we store below is newer.
    // Children won't come back on this fetch since they haven't been stored, so remove them
    // before our delegate throws a failure.
    BookmarkRecord rec0 = (BookmarkRecord) record0;
    rec0.children = new JSONArray();
    performWait(storeRunnable(session, record0));

    ExpectFetchDelegate timestampDelegate = new ExpectFetchDelegate(new Record[] { rec0 });
    performWait(fetchRunnable(session, new String[] { record0.guid }, timestampDelegate));

    BookmarkHelpers.dumpBookmarksDB(getApplicationContext());
    Record record1 = BookmarkHelpers.createBookmark1();
    Record record2 = BookmarkHelpers.createBookmark2();
    Record record3 = BookmarkHelpers.createFolder1();
    BookmarkRecord bmk3 = (BookmarkRecord) record3;
    record3.guid = Utils.generateGuid();
    record3.lastModified = timestampDelegate.records.get(0).lastModified + 3000;
    assert(!record0.guid.equals(record3.guid));

    // Store an additional record after inserting the duplicate folder
    // with new GUID. Make sure it comes back as well.
    Record record4 = BookmarkHelpers.createBookmark3();
    BookmarkRecord bmk4 = (BookmarkRecord) record4;
    bmk4.parentID = bmk3.guid;
    bmk4.parentName = bmk3.parentName;

    doStore(session, new Record[] {
      record1, record2, record3, bmk4
    });
    BookmarkRecord bmk1 = (BookmarkRecord) record1;
    bmk1.parentID = record3.guid;
    BookmarkRecord bmk2 = (BookmarkRecord) record2;
    bmk2.parentID = record3.guid;
    Record[] expect = new Record[] {
        bmk1, bmk2, record3
    };
    fetchAllRunnable(session, new ExpectFetchDelegate(expect));
  }
  
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

  @Override
  public void testCleanMultipleRecords() {
    cleanMultipleRecords(
        BookmarkHelpers.createBookmarkInMobileFolder1(),
        BookmarkHelpers.createBookmarkInMobileFolder2(),
        BookmarkHelpers.createBookmark1(),
        BookmarkHelpers.createBookmark2(),
        BookmarkHelpers.createFolder1());
  }

  public void testBasicPositioning() {
    prepSession();
    AndroidBrowserRepositorySession session = getSession();
    Record[] expected = new Record[] {
        BookmarkHelpers.createBookmark1(),
        BookmarkHelpers.createFolder1(),
        BookmarkHelpers.createBookmark2()
    };
    System.out.println("TEST: Inserting " + expected[0].guid + ", "
                                          + expected[1].guid + ", "
                                          + expected[2].guid);
    doStore(session, expected);
    
    ExpectFetchDelegate delegate = new ExpectFetchDelegate(expected);
    performWait(fetchAllRunnable(session, delegate));
    
    int found = 0;
    boolean foundFolder = false;
    for (int i = 0; i < delegate.records.size(); i++) {
      BookmarkRecord rec = (BookmarkRecord) delegate.records.get(i);
      if (rec.guid.equals(expected[0].guid)) {
        assertEquals(0, ((BookmarkRecord) delegate.records.get(i)).androidPosition);
        found++;
      } else if (rec.guid.equals(expected[2].guid)) {
        assertEquals(1, ((BookmarkRecord) delegate.records.get(i)).androidPosition);
        found++;
      } else if (rec.guid.equals(expected[1].guid)) {
        foundFolder = true;
      } else {
        System.out.println("TEST: found " + rec.guid);
      }
    }
    assertTrue(foundFolder);
    assertEquals(2, found);
  }
  
  public void testSqlInjectPurgeDeleteAndUpdateByGuid() {
    // Some setup.
    prepSession();
    AndroidBrowserRepositoryDataAccessor db = getDataAccessor();
    
    ContentValues cv = new ContentValues();
    cv.put(BrowserContract.SyncColumns.IS_DELETED, 1);
    
    // Create and insert 2 bookmarks, 2nd one is evil (attempts injection).
    BookmarkRecord bmk1 = BookmarkHelpers.createBookmark1();
    BookmarkRecord bmk2 = BookmarkHelpers.createBookmark2();
    bmk2.guid = "' or '1'='1";

    db.insert(bmk1);
    db.insert(bmk2);

    // Test 1 - updateByGuid() handles evil bookmarks correctly.
    db.updateByGuid(bmk2.guid, cv);

    // Query bookmarks table.
    Cursor cur = getAllBookmarks();
    int numBookmarks = cur.getCount();

    // Ensure only the evil bookmark is marked for deletion.
    try {
      cur.moveToFirst();
      while (!cur.isAfterLast()) {
        String guid = RepoUtils.getStringFromCursor(cur, BrowserContract.SyncColumns.GUID);
        boolean deleted = RepoUtils.getLongFromCursor(cur, BrowserContract.SyncColumns.IS_DELETED) == 1;

        if (guid.equals(bmk2.guid)) {
          assertTrue(deleted);
        } else {
          assertFalse(deleted);
        }
        cur.moveToNext();
      }
    } finally {
      cur.close();
    }

    // Test 2 - Ensure purgeDelete()'s call to delete() deletes only 1 record.
    try {
      db.purgeDeleted();
    } catch (NullCursorException e) {
      e.printStackTrace();
    }

    cur = getAllBookmarks();
    int numBookmarksAfterDeletion = cur.getCount();

    // Ensure we have only 1 deleted row.
    assertEquals(numBookmarksAfterDeletion, numBookmarks - 1);

    // Ensure only the evil bookmark is deleted.
    try {
      cur.moveToFirst();
      while (!cur.isAfterLast()) {
        String guid = RepoUtils.getStringFromCursor(cur, BrowserContract.SyncColumns.GUID);
        boolean deleted = RepoUtils.getLongFromCursor(cur, BrowserContract.SyncColumns.IS_DELETED) == 1;

        if (guid.equals(bmk2.guid)) {
          fail("Evil guid was not deleted!");
        } else {
          assertFalse(deleted);
        }
        cur.moveToNext();
      }
    } finally {
      cur.close();
    }
  }
  
  protected Cursor getAllBookmarks() {
    Context context = getApplicationContext();
    Cursor cur = context.getContentResolver().query(BrowserContract.Bookmarks.CONTENT_URI,
        BrowserContract.Bookmarks.BookmarkColumns, null, null, null);
    return cur;
  }

  public void testSqlInjectFetch() {
    // Some setup.
    prepSession();
    AndroidBrowserRepositoryDataAccessor db = getDataAccessor();

    // Create and insert 4 bookmarks, last one is evil (attempts injection).
    BookmarkRecord bmk1 = BookmarkHelpers.createBookmark1();
    BookmarkRecord bmk2 = BookmarkHelpers.createBookmark2();
    BookmarkRecord bmk3 = BookmarkHelpers.createBookmark3();
    BookmarkRecord bmk4 = BookmarkHelpers.createBookmark4();
    bmk4.guid = "' or '1'='1";

    db.insert(bmk1);
    db.insert(bmk2);
    db.insert(bmk3);
    db.insert(bmk4);

    // Perform a fetch.
    Cursor cur = null;
    try {
      cur = db.fetch(new String[] { bmk3.guid, bmk4.guid });
    } catch (NullCursorException e1) {
      e1.printStackTrace();
    }

    // Ensure the correct number (2) of records were fetched and with the correct guids.
    if (cur == null) {
      fail("No records were fetched.");
    }

    try {
      if (cur.getCount() != 2) {
        fail("Wrong number of guids fetched!");
      }
      cur.moveToFirst();
      while (!cur.isAfterLast()) {
        String guid = RepoUtils.getStringFromCursor(cur, BrowserContract.SyncColumns.GUID);
        if (!guid.equals(bmk3.guid) && !guid.equals(bmk4.guid)) {
          fail("Wrong guids were fetched!");
        }
        cur.moveToNext();
      }
    } finally {
      cur.close();
    }
  }

  public void testSqlInjectDelete() {
    // Some setup.
    prepSession();
    AndroidBrowserRepositoryDataAccessor db = getDataAccessor();

    // Create and insert 2 bookmarks, 2nd one is evil (attempts injection).
    BookmarkRecord bmk1 = BookmarkHelpers.createBookmark1();
    BookmarkRecord bmk2 = BookmarkHelpers.createBookmark2();
    bmk2.guid = "' or '1'='1";

    db.insert(bmk1);
    db.insert(bmk2);

    // Note size of table before delete.
    Cursor cur = getAllBookmarks();
    int numBookmarks = cur.getCount();

    db.delete(bmk2);

    // Note size of table after delete.
    cur = getAllBookmarks();
    int numBookmarksAfterDelete = cur.getCount();

    // Ensure size of table after delete is *only* 1 less.
    assertEquals(numBookmarksAfterDelete, numBookmarks - 1);

    try {
      cur.moveToFirst();
      while (!cur.isAfterLast()) {
        String guid = RepoUtils.getStringFromCursor(cur, BrowserContract.SyncColumns.GUID);
        if (guid.equals(bmk2.guid)) {
          fail("Guid was not deleted!");
        }
        cur.moveToNext();
      }
    } finally {
      cur.close();
    }
  }
}
