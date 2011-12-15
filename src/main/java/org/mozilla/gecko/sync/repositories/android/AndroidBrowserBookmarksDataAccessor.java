package org.mozilla.gecko.sync.repositories.android;

import org.json.simple.JSONArray;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class AndroidBrowserBookmarksDataAccessor extends AndroidBrowserRepositoryDataAccessor {

  public static final String TYPE_FOLDER = "folder";
  public static final String TYPE_BOOKMARK = "bookmark";

  public AndroidBrowserBookmarksDataAccessor(Context context) {
    super(context);
  }

  @Override
  protected Uri getUri() {
    return BrowserContract.Bookmarks.CONTENT_URI;
  }

  protected Cursor getGuidsIDsForFolders() throws NullCursorException {
    String where = BrowserContract.Bookmarks.IS_FOLDER + "=1";
    queryStart = System.currentTimeMillis();
    Cursor cur = context.getContentResolver().query(getUri(), null, where, null, null);
    queryEnd = System.currentTimeMillis();
    DBUtils.queryTimeLogger("AndroidBrowserBookmarksDataAccessor.getGuidsIDsForFolders", queryStart, queryEnd);
    if (cur == null) {
      throw new NullCursorException(null);
    }
    return cur;
  }

  protected void updateParentAndPosition(String guid, long newParentId, long position) {
    ContentValues cv = new ContentValues();
    cv.put(BrowserContract.Bookmarks.PARENT, newParentId);
    cv.put(BrowserContract.Bookmarks.POSITION, position);
    updateByGuid(guid, cv);
  } 
  
  /*
   * Verify that all special guids are present and that they aren't set to deleted.
   * Inser them if they aren't there.
   */
  public void checkAndBuildSpecialGuids() throws NullCursorException {
    Cursor cur = fetch(DBUtils.SPECIAL_GUIDS);
    cur.moveToFirst();
    int count = 0;
    boolean containsMobileFolder = false;
    long mobileRoot = 0;
    while (!cur.isAfterLast()) {
      String guid = DBUtils.getStringFromCursor(cur, BrowserContract.SyncColumns.GUID);
      if (guid.equals("mobile")) {
        containsMobileFolder = true;
        mobileRoot = DBUtils.getLongFromCursor(cur, BrowserContract.CommonColumns._ID);
      }
      count++;
      
      // Make sure none of these folders are marked as deleted
      if (DBUtils.getLongFromCursor(cur, BrowserContract.SyncColumns.IS_DELETED) == 1) {
        ContentValues cv = new ContentValues();
        cv.put(BrowserContract.SyncColumns.IS_DELETED, 0);
        updateByGuid(guid, cv);
      }
      cur.moveToNext();
    }
    cur.close();
    
    // Insert them if missing
    if (count != DBUtils.SPECIAL_GUIDS.length) {
      if (!containsMobileFolder) {
        mobileRoot = insertSpecialFolder("mobile", 0);
      }
      long desktop = insertSpecialFolder("places", mobileRoot);
      insertSpecialFolder("unfiled", desktop);
      insertSpecialFolder("menu", desktop);
      insertSpecialFolder("toolbar", desktop);
    }
  }

  private long insertSpecialFolder(String guid, long parentId) {
      BookmarkRecord record = new BookmarkRecord(guid);
      record.title = DBUtils.SPECIAL_GUIDS_MAP.get(guid);
      record.type = "folder";
      record.androidParentID = parentId;
      return(DBUtils.getAndroidIdFromUri(insert(record)));
  }

  @Override
  protected ContentValues getContentValues(Record record) {
    ContentValues cv = new ContentValues();
    BookmarkRecord rec = (BookmarkRecord) record;
    cv.put("guid",          rec.guid);
    cv.put(BrowserContract.Bookmarks.TITLE,       rec.title);
    cv.put(BrowserContract.Bookmarks.URL,         rec.bookmarkURI);
    cv.put(BrowserContract.Bookmarks.DESCRIPTION,         rec.description);
    if (rec.tags == null) {
      rec.tags = new JSONArray();
    }
    cv.put(BrowserContract.Bookmarks.TAGS,            rec.tags.toJSONString());
    cv.put(BrowserContract.Bookmarks.KEYWORD,         rec.keyword);
    cv.put(BrowserContract.Bookmarks.PARENT,          rec.androidParentID);
    cv.put(BrowserContract.Bookmarks.POSITION, rec.androidPosition);

    // NOTE: Only bookmark and folder types should make it this far,
    // other types should be filtered out and droppped
    cv.put(BrowserContract.Bookmarks.IS_FOLDER, rec.type.equalsIgnoreCase(TYPE_FOLDER) ? 1 : 0);

    cv.put("modified", rec.lastModified);
    return cv;
  }
  
  // Returns a cursor with any records that list the given androidID as a parent
  public Cursor getChildren(long androidID) throws NullCursorException {
    String where = BrowserContract.Bookmarks.PARENT + "=" + androidID;
    queryStart = System.currentTimeMillis();
    Cursor cur = context.getContentResolver().query(getUri(), getAllColumns(), where, null, null);
    queryEnd = System.currentTimeMillis();
    DBUtils.queryTimeLogger("AndroidBrowserBookmarksDataAccessor.getChildren", queryStart, queryEnd);
    if (cur == null) {
      throw new NullCursorException(null);
    }
    return cur;
  }
  
  @Override
  protected String[] getAllColumns() {
    return BrowserContract.Bookmarks.BookmarksColumns;
  }

}
