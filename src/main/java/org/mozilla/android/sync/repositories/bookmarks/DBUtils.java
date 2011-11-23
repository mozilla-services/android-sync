package org.mozilla.android.sync.repositories.bookmarks;

import org.mozilla.android.sync.repositories.domain.BookmarkRecord;

import android.database.Cursor;
import android.net.Uri;
import android.provider.Browser;

public class DBUtils {
  
  public static final String MOBILE_PARENT_ID = "mobile";
  public static final String MOBILE_PARENT_NAME = "mobile";
  public static final String BOOKMARK_TYPE = "bookmark";      
  
  public static String getStringFromCursor(Cursor cur, String colId) {
    return cur.getString(cur.getColumnIndex(colId));
  }
  
  public static long getLongFromCursor(Cursor cur, String colId) {
    return cur.getLong(cur.getColumnIndex(colId));
  }
  
  // Returns android id from the uri that we get after inserting a
  // bookmark into the local android store
  public static long getAndroidIdFromUri(Uri uri) {
    String path = uri.getPath();
    int lastSlash = path.lastIndexOf('/');
    return Long.parseLong(path.substring(lastSlash + 1));
  }
  
  //Create a BookmarkRecord object from a cursor on a row with a Moz Bookmark in it
  public static BookmarkRecord bookmarkFromMozCursor(Cursor curMoz) {

    String guid = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_GUID);
    String collection = "bookmarks";
    long lastModified = getLongFromCursor(curMoz, BookmarksDatabaseHelper.COL_LAST_MOD);

    BookmarkRecord rec = new BookmarkRecord(guid, collection, lastModified);

    rec.androidID = getLongFromCursor(curMoz, BookmarksDatabaseHelper.COL_ANDROID_ID);
    rec.title = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_TITLE);
    rec.bookmarkURI = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_BMK_URI);
    rec.description = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_DESCRIP);
    rec.loadInSidebar = curMoz.getInt(curMoz.getColumnIndex(BookmarksDatabaseHelper.COL_LOAD_IN_SIDEBAR)) == 1 ? true: false ;
    rec.tags = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_TAGS);
    rec.keyword = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_KEYWORD);
    rec.parentID = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_PARENT_ID);
    rec.parentName = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_PARENT_NAME);
    rec.type = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_TYPE);
    rec.generatorURI = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_GENERATOR_URI);
    rec.staticTitle = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_STATIC_TITLE);
    rec.folderName = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_FOLDER_NAME);
    rec.queryID = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_QUERY_ID);
    rec.siteURI = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_SITE_URI);
    rec.feedURI = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_FEED_URI);
    rec.pos = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_POS);
    rec.children = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_CHILDREN);
    return rec;
  }
 
  // Create a BookmarkRecord object from a cursor on a row with a Android stock Bookmark in it
  public static BookmarkRecord bookmarkFromAndroidCursor(Cursor curDroid) {
    String title = DBUtils.getStringFromCursor(curDroid, Browser.BookmarkColumns.TITLE);
    String uri = DBUtils.getStringFromCursor(curDroid, Browser.BookmarkColumns.URL);
    long droidId = DBUtils.getLongFromCursor(curDroid, Browser.BookmarkColumns._ID);
   
    BookmarkRecord rec = new BookmarkRecord();
    rec.androidID = droidId;
    rec.loadInSidebar = false;
    rec.title = title;
    rec.bookmarkURI = uri;
    rec.description = "";
    rec.tags = "";
    rec.keyword = "";
    rec.parentID = MOBILE_PARENT_ID;
    rec.parentName = MOBILE_PARENT_NAME;
    rec.type = BOOKMARK_TYPE;
    rec.generatorURI = "";
    rec.staticTitle = "";
    rec.folderName = "";
    rec.queryID = "";
    rec.siteURI = "";
    rec.feedURI = "";
    rec.pos = "";
    rec.children = "";
    return rec;    
 }

}