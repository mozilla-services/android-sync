package org.mozilla.android.sync.repositories;

import org.mozilla.android.sync.repositories.domain.Record;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

public abstract class RepositoryDatabaseHelper extends SQLiteOpenHelper {

  // Database Specifications
  protected static final String DB_NAME             = "bookmarks_database";
  protected static final int    SCHEMA_VERSION      = 1;

  // Shared columns (must exist in all tables)
  public static final String    COL_GUID            = "guid";
  public static final String    COL_ID              = "id";
  public static final String    COL_LAST_MOD        = "modified";
  public static final String    COL_DELETED         = "deleted";

  // Bookmarks Table
  public static final String    TBL_BOOKMARKS       = "Bookmarks";
  public static final String    COL_ANDROID_ID      = "androidID";
  public static final String    COL_TITLE           = "title";
  public static final String    COL_BMK_URI         = "bmkURI";
  public static final String    COL_DESCRIP         = "description";
  public static final String    COL_LOAD_IN_SIDEBAR = "loadInSidebar";
  public static final String    COL_TAGS            = "tags";
  public static final String    COL_KEYWORD         = "keyword";
  public static final String    COL_PARENT_ID       = "parentID";
  public static final String    COL_PARENT_NAME     = "parentName";
  public static final String    COL_TYPE            = "type";
  public static final String    COL_GENERATOR_URI   = "generatorUri";
  public static final String    COL_STATIC_TITLE    = "staticTitle";
  public static final String    COL_FOLDER_NAME     = "folderName";
  public static final String    COL_QUERY_ID        = "queryId";
  public static final String    COL_SITE_URI        = "siteUri";
  public static final String    COL_FEED_URI        = "feedUri";
  public static final String    COL_POS             = "pos";
  public static final String    COL_CHILDREN        = "children";

  protected RepositoryDatabaseHelper(Context context) {
    super(context, DB_NAME, null, SCHEMA_VERSION);
  }

  public void onCreate(SQLiteDatabase db) {
    String createBookmarksSql = "CREATE TABLE " + TBL_BOOKMARKS + " (" + COL_ID
        + " INTEGER PRIMARY KEY AUTOINCREMENT, " + COL_GUID + " TEXT, "
        + COL_ANDROID_ID + " INTEGER, " + COL_TITLE + " TEXT, " + COL_BMK_URI
        + " TEXT, " + COL_DESCRIP + " TEXT, " + COL_LOAD_IN_SIDEBAR
        + " INTEGER, " + COL_TAGS + " TEXT, " + COL_KEYWORD + " TEXT, "
        + COL_PARENT_ID + " TEXT, " + COL_PARENT_NAME + " TEXT, " + COL_TYPE
        + " TEXT, " + COL_GENERATOR_URI + " TEXT, " + COL_STATIC_TITLE
        + " TEXT, " + COL_FOLDER_NAME + " TEXT, " + COL_QUERY_ID + " TEXT, "
        + COL_SITE_URI + " TEXT, " + COL_FEED_URI + " TEXT, " + COL_POS
        + " TEXT, " + COL_CHILDREN + " TEXT, " + COL_LAST_MOD + " INTEGER, "
        + COL_DELETED + " INTEGER DEFAULT 0)";

    db.execSQL(createBookmarksSql);
  }

  // Cache these so we don't have to track them across cursors. Call `close`
  // when you're done.
  private static SQLiteDatabase readableDatabase;
  private static SQLiteDatabase writableDatabase;

  protected SQLiteDatabase getCachedReadableDatabase() {
    if (RepositoryDatabaseHelper.readableDatabase == null) {
      if (RepositoryDatabaseHelper.writableDatabase == null) {
        RepositoryDatabaseHelper.readableDatabase = this.getReadableDatabase();
        return RepositoryDatabaseHelper.readableDatabase;
      } else {
        return RepositoryDatabaseHelper.writableDatabase;
      }
    } else {
      return RepositoryDatabaseHelper.readableDatabase;
    }
  }

  protected SQLiteDatabase getCachedWritableDatabase() {
    if (RepositoryDatabaseHelper.writableDatabase == null) {
      RepositoryDatabaseHelper.writableDatabase = this.getWritableDatabase();
    }
    return RepositoryDatabaseHelper.writableDatabase;
  }

  public void close() {
    if (RepositoryDatabaseHelper.readableDatabase != null) {
      RepositoryDatabaseHelper.readableDatabase.close();
      RepositoryDatabaseHelper.readableDatabase = null;
    }
    if (RepositoryDatabaseHelper.writableDatabase != null) {
      RepositoryDatabaseHelper.writableDatabase.close();
      RepositoryDatabaseHelper.writableDatabase = null;
    }
    super.close();
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // For now we'll just drop and recreate the tables
    db.execSQL("DROP TABLE IF EXISTS " + TBL_BOOKMARKS);
    onCreate(db);
  }

  public void wipe() throws SQLiteException {
    SQLiteDatabase db = this.getCachedWritableDatabase();
    onUpgrade(db, SCHEMA_VERSION, SCHEMA_VERSION);
  }

  // mark a record deleted
  public void markDeleted(String guid) {
    ContentValues cv = new ContentValues();
    cv.put(COL_DELETED, 1);
    updateByGuid(guid, cv);
  }

  public abstract void updateByGuid(String guid, ContentValues cv);
  public abstract long insert(Record record);
  public abstract Cursor fetchAllOrderByAndroidId();
  public abstract Cursor getGUIDSSince(long timestamp);
  public abstract Cursor fetchSince(long timestamp);
  public abstract Cursor fetch(String guids[]);
  public abstract void delete(Record record);
}
