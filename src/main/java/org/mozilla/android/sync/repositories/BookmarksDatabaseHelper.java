package org.mozilla.android.sync.repositories;

import org.mozilla.android.sync.repositories.domain.BookmarkRecord;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

public class BookmarksDatabaseHelper extends SQLiteOpenHelper {

  private static final String DB_NAME       = "bookmarksPOC";
  private static final int SCHEMA_VERSION = 1;

  // Bookmarks table
  // Wondering how much of this is actually set on mobile?
  // For example do we have separators, livemarks, etc?
  // tags and children are technically supposed to be an
  // array of strings...this might work for now since I'm
  // not sure we will ever actually access them on the mobile end
  public static final String TBL_BOOKMARKS = "Bookmarks";
  public static final String COL_ID = "id";
  public static final String COL_GUID = "guid";
  public static final String COL_ANDROID_ID = "androidID";
  public static final String COL_TITLE     = "title";
  public static final String COL_BMK_URI   = "bmkURI";
  public static final String COL_DESCRIP   = "description";
  public static final String COL_LOAD_IN_SIDEBAR = "loadInSidebar";
  public static final String COL_TAGS = "tags";
  public static final String COL_KEYWORD = "keyword";
  public static final String COL_PARENT_ID = "parentID";
  public static final String COL_PARENT_NAME = "parentName";
  public static final String COL_TYPE = "type";
  public static final String COL_GENERATOR_URI = "generatorUri";
  public static final String COL_STATIC_TITLE = "staticTitle";
  public static final String COL_FOLDER_NAME = "folderName";
  public static final String COL_QUERY_ID = "queryId";
  public static final String COL_SITE_URI = "siteUri";
  public static final String COL_FEED_URI = "feedUri";
  public static final String COL_POS = "pos";
  public static final String COL_CHILDREN = "children";
  public static final String COL_LAST_MOD = "modified";
  public static final String COL_DELETED = "deleted";

  private static String[] BOOKMARKS_COLUMNS;

  // TODO History table

  public BookmarksDatabaseHelper(Context context) {
    super(context, DB_NAME, null, SCHEMA_VERSION);
    BOOKMARKS_COLUMNS = new String[] {
        COL_ID, COL_GUID, COL_ANDROID_ID, COL_TITLE,
        COL_BMK_URI, COL_DESCRIP, COL_LOAD_IN_SIDEBAR,
        COL_TAGS, COL_KEYWORD, COL_PARENT_ID, COL_PARENT_NAME,
        COL_TYPE, COL_GENERATOR_URI, COL_STATIC_TITLE,
        COL_FOLDER_NAME, COL_QUERY_ID, COL_SITE_URI,
        COL_FEED_URI, COL_POS, COL_CHILDREN, COL_LAST_MOD,
        COL_DELETED};
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    String createBookmarksSql =
        "CREATE TABLE " + TBL_BOOKMARKS + " (" +
        COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
        COL_GUID + " TEXT, " +
        COL_ANDROID_ID + " INTEGER, " +
        COL_TITLE + " TEXT, " +
        COL_BMK_URI + " TEXT, " +
        COL_DESCRIP + " TEXT, " +
        COL_LOAD_IN_SIDEBAR + " INTEGER, " +
        COL_TAGS + " TEXT, " +
        COL_KEYWORD + " TEXT, " +
        COL_PARENT_ID + " TEXT, " +
        COL_PARENT_NAME + " TEXT, " +
        COL_TYPE + " TEXT, " +
        COL_GENERATOR_URI + " TEXT, " +
        COL_STATIC_TITLE + " TEXT, " +
        COL_FOLDER_NAME + " TEXT, " +
        COL_QUERY_ID+ " TEXT, " +
        COL_SITE_URI + " TEXT, " +
        COL_FEED_URI + " TEXT, " +
        COL_POS + " TEXT, " +
        COL_CHILDREN + " TEXT, " +
        COL_LAST_MOD + " INTEGER, " +
        COL_DELETED + " INTEGER DEFAULT 0)";

    db.execSQL(createBookmarksSql);

  }

  /*
   * TODO: These next two methods scare me a bit...
   */
  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // For now we'll just drop and recreate the tables
    db.execSQL("DROP TABLE IF EXISTS " + TBL_BOOKMARKS);
    onCreate(db);
  }

  public void wipe() throws SQLiteException {
    SQLiteDatabase db = this.getWritableDatabase();
    onUpgrade(db, SCHEMA_VERSION, SCHEMA_VERSION);
  }

  // inserts and return the row id for the bookmark
  public long insertBookmark(BookmarkRecord bookmark) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues cv = getContentValues(bookmark);
    long rowId = db.insert(TBL_BOOKMARKS, null, cv);

    Log.i("DBLocal", "Inserted bookmark into row: " + rowId);

    return rowId;
  }

  // returns all bookmarks records
  public Cursor getAllBookmarks() {
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor cur = db.query(TBL_BOOKMARKS, BOOKMARKS_COLUMNS, null, null, null, null, null);
    return cur;
  }

  // get all guids modified since timestamp
  public Cursor getGUIDSSince(long timestamp) {
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor c = db.query(TBL_BOOKMARKS, new String[] {COL_GUID}, COL_LAST_MOD + " >= " +
        Long.toString(timestamp), null, null, null, null);
    return c;
  }

  // get records modified since timestamp
  public Cursor fetchSince(long timestamp) {
    SQLiteDatabase db = this.getReadableDatabase();
    Cursor c = db.query(TBL_BOOKMARKS, BOOKMARKS_COLUMNS, COL_LAST_MOD + " >= " +
        Long.toString(timestamp), null, null, null, null);
    return c;
  }

  // get all records requested
  public Cursor fetch(String guids[]) {
    SQLiteDatabase db = this.getReadableDatabase();
    String where = COL_GUID + " in (";
    for (String guid : guids) {
      where = where + "'" + guid + "', ";
    }
    where = (where.substring(0, where.length() -2) + ")");
    String queryString = SQLiteQueryBuilder.buildQueryString(false, TBL_BOOKMARKS, BOOKMARKS_COLUMNS, where, null, null, null, null);
    return db.rawQuery(queryString, null);
  }

  // update and return number of rows affected
  public int updateBookmark(BookmarkRecord bookmark) {
    SQLiteDatabase db = this.getWritableDatabase();
    ContentValues cv = getContentValues(bookmark);
    int rows = db.update(TBL_BOOKMARKS, cv, COL_ID + " =? ", new String[]
        {String.valueOf(bookmark.getId())});

    Log.i("DBLocal", "Updated " + rows + " bookmarks rows");
    return rows;
  }

  // delete rows
  // This actually deletes the bookmark from the DB
  public void deleteBookmark(BookmarkRecord bookmark) {
    SQLiteDatabase db = this.getWritableDatabase();
    db.delete(TBL_BOOKMARKS, COL_ID+"=?", new String[]
        {String.valueOf(bookmark.getId())});
  }

  private ContentValues getContentValues(BookmarkRecord record) {
    ContentValues cv = new ContentValues();
    cv.put(COL_GUID, record.getGuid());
    cv.put(COL_ANDROID_ID, record.getAndroidId());
    cv.put(COL_TITLE, record.getTitle());
    cv.put(COL_BMK_URI, record.getBmkUri());
    cv.put(COL_DESCRIP, record.getDescription());
    cv.put(COL_LOAD_IN_SIDEBAR, record.getLoadInSidebar() ? 1 : 0);
    cv.put(COL_TAGS, record.getTags());
    cv.put(COL_KEYWORD, record.getKeyword());
    cv.put(COL_PARENT_ID, record.getParentId());
    cv.put(COL_PARENT_NAME, record.getParentName());
    cv.put(COL_TYPE, record.getType());
    cv.put(COL_GENERATOR_URI, record.getGeneratorUri());
    cv.put(COL_STATIC_TITLE, record.getStaticTitle());
    cv.put(COL_FOLDER_NAME, record.getFolderName());
    cv.put(COL_QUERY_ID, record.getQueryId());
    cv.put(COL_SITE_URI, record.getSiteUri());
    cv.put(COL_FEED_URI, record.getFeedUri());
    cv.put(COL_POS, record.getPos());
    cv.put(COL_CHILDREN, record.getChildren());
    cv.put(COL_LAST_MOD, System.currentTimeMillis()/1000);

    return cv;
  }

}
