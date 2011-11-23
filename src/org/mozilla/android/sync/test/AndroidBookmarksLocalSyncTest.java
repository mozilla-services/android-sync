/* Any copyright is dedicated to the Public Domain.
   http://creativecommons.org/publicdomain/zero/1.0/ */

package org.mozilla.android.sync.test;

import org.mozilla.android.sync.MainActivity;
import org.mozilla.android.sync.repositories.bookmarks.BookmarksDatabaseHelper;
import org.mozilla.android.sync.repositories.bookmarks.DBUtils;
import org.mozilla.android.sync.repositories.bookmarks.LocalBookmarkSynchronizer;
import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.test.helpers.BookmarkHelpers;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Browser;
import android.test.ActivityInstrumentationTestCase2;
import android.util.Log;

public class AndroidBookmarksLocalSyncTest extends
    ActivityInstrumentationTestCase2<MainActivity> {
  
  public AndroidBookmarksLocalSyncTest() {
    super(MainActivity.class);
  }

  private static Context context; 
  private static BookmarksDatabaseHelper helper;
  
  public Context getApplicationContext() {
    if (context == null) {
      context = this.getInstrumentation().getTargetContext().getApplicationContext();
    }
    return context;
  }


  private void wipe() {
    if (helper == null) {
      helper = new BookmarksDatabaseHelper(getApplicationContext());
    }
    helper.wipe();
    
    // Wipe stock android db
    Log.i("jvoll", "Wiping stock bookmarks db");
    getApplicationContext().getContentResolver().delete(Browser.BOOKMARKS_URI, null, null);
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
   * Tests for syncing from android stock bookmark db
   * to Mozilla's local db on the android device
   */
  
  // Test adding bookmarks from stock db to moz db
  public void testGetNewBookmarksFromStock() {
    
    // Write 2 bookmarks into stock browser db
    ContentValues[] expected = new ContentValues[2];
    expected[0] = getContentValuesStock("BK1", "http://www.bk1.com");
    expected[1] = getContentValuesStock("BK2", "http://www.bk2.com");
    context.getContentResolver().insert(Browser.BOOKMARKS_URI , expected[0]);
    context.getContentResolver().insert(Browser.BOOKMARKS_URI , expected[1]);
    
    // Perform a sync to local db
    LocalBookmarkSynchronizer sync = new LocalBookmarkSynchronizer(context);
    sync.syncStockToMoz();
    
    // Get records from local db, verify both are there
    Cursor cur = helper.fetchAllBookmarksOrderByAndroidId();
    cur.moveToFirst();
    int count = 0;
    
    while (!cur.isAfterLast()) {
      String title = DBUtils.getStringFromCursor(cur, BookmarksDatabaseHelper.COL_TITLE);
      String url = DBUtils.getStringFromCursor(cur, BookmarksDatabaseHelper.COL_BMK_URI);
      
      // Check to see if this bookmark matches one of ours
      for (int i = 0; i < expected.length; i++) {
        if (title.equals(expected[i].getAsString(Browser.BookmarkColumns.TITLE)) &&
            url.equals(expected[i].getAsString(Browser.BookmarkColumns.URL))) {
          count++;
        }
      }
      cur.moveToNext();
    }
    
    // There are other bookmarks already in there by default from android.
    // Make sure ours are found for sure though.
    assertEquals(2, count);
    
    cur.close();
  }
  
  // Test deletion of a stock bookmark
  public void testDeleteBookmarkFromStock() {
    
    BookmarkRecord[] records = new BookmarkRecord[] {
      BookmarkHelpers.createBookmark1(),
      BookmarkHelpers.createBookmark2()
    };
    
    // Add 1 of the 2 bookmarks to the stock db
    ContentValues cv = getContentValuesStock(records[0].title, records[0].bookmarkURI);
    Uri resourceUri = context.getContentResolver().insert(Browser.BOOKMARKS_URI, cv);
    long androidId = DBUtils.getAndroidIdFromUri(resourceUri);
    records[0].androidID = androidId;
    records[1].androidID = androidId + 1;
    
    // Add both bookmarks to moz db
    helper.insertBookmark(records[0]);
    helper.insertBookmark(records[1]);
    
    // Perform a sync to local db
    LocalBookmarkSynchronizer sync = new LocalBookmarkSynchronizer(context);
    sync.syncStockToMoz();
    
    // Verify that one record is marked as deleted and other not in moz db
    Cursor cur = helper.fetch(new String[] { records[0].guid, records[1].guid } );
    cur.moveToFirst();
    
    for (int i = 0; i < 2; i++) {
      assertEquals(records[i].guid, DBUtils.getStringFromCursor(cur, BookmarksDatabaseHelper.COL_GUID));
      assertEquals(records[i].title, DBUtils.getStringFromCursor(cur, BookmarksDatabaseHelper.COL_TITLE));
      long deleted = DBUtils.getLongFromCursor(cur, BookmarksDatabaseHelper.COL_DELETED);
      
      if (i == 0) {
        assertEquals(0, deleted);
      } else {
        assertEquals(1, deleted);
      }
      cur.moveToNext();
    }
    
    cur.close();
  }
  
  // Test modification of a bookmark in the stock db
  public void testModifiedBookmarkInStock() {
    BookmarkRecord[] records = new BookmarkRecord[] {
        BookmarkHelpers.createBookmark1(),
        BookmarkHelpers.createBookmark2()
    };
    
    // Add both to moz snapshot
    helper.insertBookmark(records[0]);
    helper.insertBookmark(records[1]);
    
    // Modify the title of one of the bookmarks and
    // the url of the other and insert into stock db
    records[0].title = "New title";
    records[1].bookmarkURI = "http://uri.new.com";
    storeToStock(records[0]);
    storeToStock(records[1]);
    
    // Perform a sync to local db
    LocalBookmarkSynchronizer sync = new LocalBookmarkSynchronizer(context);
    sync.syncStockToMoz();
    
    // Verification step
    Cursor cur = helper.fetchAllBookmarksOrderByAndroidId();
    cur.moveToFirst();
    BookmarkHelpers.verifyExpectedRecordReturned(records[0], DBUtils.bookmarkFromMozCursor(cur));
    cur.moveToNext();
    BookmarkHelpers.verifyExpectedRecordReturned(records[1], DBUtils.bookmarkFromMozCursor(cur));
    
    cur.close();
  }
  
  /*
   * Tests for syncing from Mozilla's local bookmark db
   * to the android stock bookmarks database
   */
  
  // Test deletion of a bookmark from moz (marked deleted),
  // bookmark exists in android stock db
  public void testDeleteBookmarkFromMoz() {
    BookmarkRecord[] records = new BookmarkRecord[] {
      BookmarkHelpers.createBookmark1(),
      BookmarkHelpers.createBookmark2()
    };
    
    // Add both bookmarks to both db's and mark one
    // deleted in local moz db
    helper.insertBookmark(records[0]);
    helper.insertBookmark(records[1]);
    storeToStock(records[0]);
    storeToStock(records[1]);
    helper.markDeleted(records[0].guid);
    
    // Perform a sync to  db
    LocalBookmarkSynchronizer sync = new LocalBookmarkSynchronizer(context);
    sync.syncMozToStock(new String[] { records[0].guid, records[1].guid } );
    
    // Verify one record is deleted from stock db and it is the correct one
    Cursor cur = context.getContentResolver().query(Browser.BOOKMARKS_URI, null, null, null, null);
    cur.moveToFirst();
    while (!cur.isAfterLast()) {
      assertEquals(records[1].title, DBUtils.getStringFromCursor(cur, Browser.BookmarkColumns.TITLE));
      assertEquals(records[1].bookmarkURI, DBUtils.getStringFromCursor(cur, Browser.BookmarkColumns.URL));
      cur.moveToNext();
    }
    
    cur.close();
  }
  
  // Two records in moz db (one with deleted flag set),
  // none in stock db
  public void testDeleteAddBookmarkFromMoz() {
    BookmarkRecord[] records = new BookmarkRecord[] {
      BookmarkHelpers.createBookmark1(),
      BookmarkHelpers.createBookmark2()
    };
    
    // Add both bookmarks to local moz db and mark one deleted
    helper.insertBookmark(records[0]);
    helper.insertBookmark(records[1]);
    helper.markDeleted(records[0].guid);
    
    // Perform a sync to  db
    LocalBookmarkSynchronizer sync = new LocalBookmarkSynchronizer(context);
    sync.syncMozToStock(new String[] { records[0].guid, records[1].guid } );
    
    // Verify only one record is added to stock db and it is the correct one
    Cursor cur = context.getContentResolver().query(Browser.BOOKMARKS_URI, null, null, null, null);
    cur.moveToFirst();
    int count = 0;
    while (!cur.isAfterLast()) {
      assertEquals(records[1].title, DBUtils.getStringFromCursor(cur, Browser.BookmarkColumns.TITLE));
      assertEquals(records[1].bookmarkURI, DBUtils.getStringFromCursor(cur, Browser.BookmarkColumns.URL));
      count ++;
      cur.moveToNext();
    }
    assertEquals(1, count);
    
    cur.close();
  }
  
  // Test adding a record to moz db, not yet in stock db
  public void testInsertFromMoz() {
    BookmarkRecord record = BookmarkHelpers.createBookmark1();
    helper.insertBookmark(record);
    
    // Perform a sync to  db
    LocalBookmarkSynchronizer sync = new LocalBookmarkSynchronizer(context);
    sync.syncMozToStock(new String[] { record.guid } );
    
    // Verify record is added to stock db
    Cursor cur = context.getContentResolver().query(Browser.BOOKMARKS_URI, null, null, null, null);
    cur.moveToFirst();
    int count = 0;
    while (!cur.isAfterLast()) {
      assertEquals(record.title, DBUtils.getStringFromCursor(cur, Browser.BookmarkColumns.TITLE));
      assertEquals(record.bookmarkURI, DBUtils.getStringFromCursor(cur, Browser.BookmarkColumns.URL));
      count ++;
      cur.moveToNext();
    }
    assertEquals(1, count);
    cur.close();
    
    // Verify that an android id has been set in the moz db
    cur = helper.fetch(new String[] { record.guid });
    cur.moveToFirst();
    long androidId = DBUtils.getLongFromCursor(cur, BookmarksDatabaseHelper.COL_ANDROID_ID);
    assertEquals(1, androidId);
    
    cur.close();
  }
  
  // Test modifiying bookmarks that already exist locally
  public void testUpdateFromMoz() {
    BookmarkRecord[] records = new BookmarkRecord[] {
      BookmarkHelpers.createBookmark1(),
      BookmarkHelpers.createBookmark2()
    };
    
    // Add both to moz snapshot and stock
    helper.insertBookmark(records[0]);
    helper.insertBookmark(records[1]);
    storeToStock(records[0]);
    storeToStock(records[1]);
    
    // Modify the title of one of the bookmarks and
    // the url of the other and insert into stock db
    records[0].title = "New title";
    records[1].bookmarkURI = "http://uri.new.com";
    helper.updateTitleUri(records[0].guid, records[0].title, records[0].bookmarkURI);
    helper.updateTitleUri(records[1].guid, records[1].title, records[1].bookmarkURI);
    
    // Perform a sync to  db
    LocalBookmarkSynchronizer sync = new LocalBookmarkSynchronizer(context);
    sync.syncMozToStock(new String[] { records[0].guid, records[1].guid } );
    
    // Verification step
    Cursor cur = context.getContentResolver().query(Browser.BOOKMARKS_URI, null, null, null, null); 
    cur.moveToFirst();
    verifyNewMozBookmarkFromStock(records[0], DBUtils.bookmarkFromAndroidCursor(cur));
    cur.moveToNext();
    verifyNewMozBookmarkFromStock(records[1], DBUtils.bookmarkFromAndroidCursor(cur));
    
    cur.close();
  }
  
  // TESTS TO ADD/MAKE SURE ARE COVERED
  // TODO test with other non-bookmark bookmarks to make sure they
  // aren't involved in local sync at all
  // TODO test that bookmarks only touched in stock if modified in moz (if not already done above)
  // TODO verify parentId and parentName correct
  
  private void storeToStock(BookmarkRecord record) {
    ContentValues cv = getContentValuesStock(record.title, record.bookmarkURI);
    Uri resourceUri = context.getContentResolver().insert(Browser.BOOKMARKS_URI, cv);
    long androidId = DBUtils.getAndroidIdFromUri(resourceUri);
    helper.updateAndroidId(record.guid, androidId);
  }
  
  private ContentValues getContentValuesStock(String title, String uri) {
    ContentValues cv = new ContentValues();
    cv.put(Browser.BookmarkColumns.BOOKMARK, 1);
    cv.put(Browser.BookmarkColumns.TITLE, title);
    cv.put(Browser.BookmarkColumns.URL, uri);
    // Making assumption that android's db has defaults for the other fields
    return cv;
  }
  
  // Check for a newly created moz bookmark that was created from a stock bookmark
  private void verifyNewMozBookmarkFromStock(BookmarkRecord expected, BookmarkRecord actual) {
    assertEquals(expected.title, actual.title);
    assertEquals(expected.bookmarkURI, actual.bookmarkURI);
    assertEquals(DBUtils.BOOKMARK_TYPE, actual.type);
    assertEquals(DBUtils.MOBILE_PARENT_ID, actual.parentID);
    assertEquals(DBUtils.MOBILE_PARENT_NAME, actual.parentName);
    assert(!actual.guid.equalsIgnoreCase(""));
    assert(actual.androidID > 0);
  }

}