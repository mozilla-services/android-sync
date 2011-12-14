package org.mozilla.gecko.sync.repositories.android;

import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;
import org.mozilla.gecko.sync.repositories.NullCursorException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Browser.BookmarkColumns;
import android.util.Log;

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
    queryTimeLogger("AndroidBrowserBookmarksDataAccessor.getGuidsIDsForFolders");
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

  public void checkAndBuildSpecialGuids() throws NullCursorException {
    // Do check. If any are missing, insert them all.
    // TODO mobile should always exist as the root,
    // remove this once that is true.

    Cursor cur = fetch(DBUtils.SPECIAL_GUIDS);
    cur.moveToFirst();
    int count = 0;
    boolean containsMobileFolder = false;
    long mobileRoot = 0;
    while (!cur.isAfterLast()) {
      if (DBUtils.getStringFromCursor(cur, BrowserContract.Bookmarks.GUID).equals("mobile")) {
        containsMobileFolder = true;
        mobileRoot = DBUtils.getLongFromCursor(cur, BrowserContract.CommonColumns._ID);
      }
      count++;
      cur.moveToNext();
    }
    cur.close();
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
    //cv.put(COL_TAGS,            rec.tags);
    //cv.put(COL_KEYWORD,         rec.keyword);
    cv.put(BrowserContract.Bookmarks.PARENT,          rec.androidParentID);
    cv.put(BrowserContract.Bookmarks.POSITION, rec.androidPosition);

    // NOTE: Only bookmark and folder types should make it this far,
    // other types should be filtered out and droppped
    cv.put(BrowserContract.Bookmarks.IS_FOLDER, rec.type.equalsIgnoreCase(TYPE_FOLDER) ? 1 : 0);

    // TODO deal with positioning stuff
    //cv.put(BrowserContract.Bookmarks.POSITION,        rec.pos);
    cv.put("modified", rec.lastModified);
    return cv;
  }

  // Returns a cursor with any records that list the given androidID as a parent
  public Cursor getChildren(long androidID) throws NullCursorException {
    String where = BrowserContract.Bookmarks.PARENT + "=" + androidID;
    queryStart = System.currentTimeMillis();
    Cursor cur = context.getContentResolver().query(getUri(), null, where, null, null);
    queryEnd = System.currentTimeMillis();
    queryTimeLogger("AndroidBrowserBookmarksDataAccessor.getChildren");
    if (cur == null) {
      throw new NullCursorException(null);
    }
    return cur;
  }

  /*
  @Override
  protected String[] getAllColumns() {
    return new String[] {
        BrowserContract.Bookmarks.GUID,
        BrowserContract.Bookmarks.POSITION,
        BrowserContract.Bookmarks.IS_FOLDER,
        BrowserContract.Bookmarks.TITLE,
        BrowserContract.Bookmarks._ID,
        BrowserContract.Bookmarks.DATE_CREATED,
        BrowserContract.Bookmarks.PARENT,
        BrowserContract.Bookmarks.URL,
        BrowserContract.Bookmarks.DATE_MODIFIED
    };

    //bookmarks.guid AS guid, position, folder, title, thumbnail, bookmarks._id AS _id, bookmarks.created AS created, favicon, parent, url, bookmarks.modified AS modified
  }
  */

  @Override
  protected String getGuidColumn() {
    return BrowserContract.Bookmarks.GUID;
  }

  @Override
  protected String getDateModifiedColumn() {
    return BrowserContract.Bookmarks.DATE_MODIFIED;
  }

  @Override
  protected String getDeletedColumn() {
    Log.e(tag, "This column doesn't exist yet in their schema");
    return null;
  }

  @Override
  protected String getAndroidIDColumn() {
    // TODO Auto-generated method stub
    return null;
  }

}
