package org.mozilla.android.sync.repositories.android;

import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.repositories.domain.Record;

import android.content.ContentValues;
import android.content.Context;
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
  
  @Override
  protected ContentValues getContentValues(Record record) {
    ContentValues cv = new ContentValues();
    BookmarkRecord rec = (BookmarkRecord) record;
    cv.put(BrowserContract.SyncColumns.GUID,          rec.guid);
    cv.put(BrowserContract.CommonColumns.TITLE,       rec.title);
    cv.put(BrowserContract.CommonColumns.URL,         rec.bookmarkURI);
    //cv.put(BrowserContract.Bookmarks.,         rec.description);
    //cv.put(BrowserContract. , rec.loadInSidebar ? 1 : 0);
    //cv.put(COL_TAGS,            rec.tags);
    //cv.put(COL_KEYWORD,         rec.keyword);
    cv.put(BrowserContract.Bookmarks.PARENT,          rec.parentID);
    //cv.put(COL_PARENT_NAME,     rec.parentName);
//    cv.put(COL_TYPE,            rec.type);
    // NOTE: Only bookmark and folder types should make it this far,
    // other types should be filtered out and droppped
    cv.put(BrowserContract.Bookmarks.IS_FOLDER, rec.type.equalsIgnoreCase(TYPE_FOLDER) ? 1 : 0);
//    cv.put(COL_GENERATOR_URI,   rec.generatorURI);
//    cv.put(COL_STATIC_TITLE,    rec.staticTitle);
//    cv.put(COL_FOLDER_NAME,     rec.folderName);
//    cv.put(COL_QUERY_ID,        rec.queryID);
//    cv.put(COL_SITE_URI,        rec.siteURI);
//    cv.put(COL_FEED_URI,        rec.feedURI);
    cv.put(BrowserContract.Bookmarks.POSITION,        rec.pos);
    //cv.put(COL_CHILDREN,        rec.children);
    cv.put(BrowserContract.SyncColumns.DATE_MODIFIED, rec.lastModified);
//    cv.put(COL_DELETED,         rec.deleted);
    return cv;
  }
  
  // Missing columns they need:
//  Description
//  LoadInSidebar
//  Tags
//  Keyword
//  Break Parent into parent_id and parent_name
//  Type
//  generator ui
//  static title
//  folder name
//  query id
//  site uri
//  feed uri
//  children
//  deleted
  
}