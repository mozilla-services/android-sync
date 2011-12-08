package org.mozilla.android.sync.repositories.android;

import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.repositories.domain.Record;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class AndroidBrowserBookmarksDataAccessor extends AndroidBrowserRepositoryDataAccessor {
  
  private static final Uri PROVIDER_URI = Uri.parse("content://org.mozilla.gecko.providers.browser/bookmarks");
  public static final String TYPE_FOLDER = "folder";
  public static final String TYPE_BOOKMARK = "bookmark";
  
  public AndroidBrowserBookmarksDataAccessor(Context context) {
    super(context);
  }
  
  @Override
  protected Uri getUri() {
    return PROVIDER_URI;
  } 
  
  protected Cursor getGuidsIDsForFolders() {
    return context.getContentResolver().query(getUri(), 
        new String[] { BrowserContract.SyncColumns.GUID, BrowserContract.CommonColumns._ID },
        BrowserContract.Bookmarks.IS_FOLDER + "=1", null, null);
  }
  
  protected void updateParent(String guid, long newParentId) {
    ContentValues cv = new ContentValues();
    cv.put(BrowserContract.Bookmarks.PARENT, newParentId);
    updateByGuid(guid, cv);
  } 
  
  @Override
  protected ContentValues getContentValues(Record record) {
    ContentValues cv = new ContentValues();
    BookmarkRecord rec = (BookmarkRecord) record;
    cv.put(BrowserContract.SyncColumns.GUID,          rec.guid);
    cv.put(BrowserContract.CommonColumns.TITLE,       rec.title);
    cv.put(BrowserContract.CommonColumns.URL,         rec.bookmarkURI);
    //cv.put(BrowserContract.Bookmarks.,         rec.description);
    //cv.put(COL_TAGS,            rec.tags);
    //cv.put(COL_KEYWORD,         rec.keyword);
    cv.put(BrowserContract.Bookmarks.PARENT,          rec.androidParentID);
    
    // NOTE: Only bookmark and folder types should make it this far,
    // other types should be filtered out and droppped
    cv.put(BrowserContract.Bookmarks.IS_FOLDER, rec.type.equalsIgnoreCase(TYPE_FOLDER) ? 1 : 0);

    // TODO deal with positioning stuff
    //cv.put(BrowserContract.Bookmarks.POSITION,        rec.pos);
    cv.put(BrowserContract.SyncColumns.DATE_MODIFIED, rec.lastModified);
    return cv;
  }
  
}