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

public class TestLocalSync extends
    ActivityInstrumentationTestCase2<MainActivity> {
  
  public TestLocalSync() {
    super(MainActivity.class);
  }

  // Think this is necessary so that the context is consistent
  private static Context context; 
  public Context getApplicationContext() {
    if (context == null) {
      context = this.getInstrumentation().getTargetContext().getApplicationContext();
    }
    return context;
  }

  private static BookmarksDatabaseHelper helper;

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
    
  }
  
  
  // TODO test with other non-bookmark bookmarks to make sure they
  // aren't involved in local sync at all
  
  // TODO check that we are saving android id's out when we insert new records into local storage
  
  private ContentValues getContentValuesStock(String title, String uri) {
    ContentValues cv = new ContentValues();
    cv.put(Browser.BookmarkColumns.BOOKMARK, 1);
    cv.put(Browser.BookmarkColumns.TITLE, title);
    cv.put(Browser.BookmarkColumns.URL, uri);
    // Making assumption that android's db has defaults for the other fields
    return cv;
  }

}
