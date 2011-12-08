/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Android Sync Client.
 *
 * The Initial Developer of the Original Code is
 * the Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 * Jason Voll <jvoll@mozilla.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.android.sync.repositories.android;

import org.mozilla.android.sync.repositories.domain.Record;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

public abstract class AndroidBrowserRepositoryDatabaseHelper extends SQLiteOpenHelper {

  public static final String    TAG                 = "AndroidBrowserRepositoryDatabaseHelper";
  
  // Database Specifications
  protected static final String DB_NAME             = "bookmarks_database";
  protected static final int    SCHEMA_VERSION      = 1;

  // Shared columns that must exist in all tables
  public static final String    COL_GUID            = "guid";
  public static final String    COL_ID              = "id";
  public static final String    COL_LAST_MOD        = "modified";
  public static final String    COL_DELETED         = "deleted";
  
  // Columns shared between Bookmarks and History tables
  public static final String    COL_ANDROID_ID      = "androidID";
  public static final String    COL_TITLE           = "title";

  // Bookmarks Table
  public static final String    TBL_BOOKMARKS       = "Bookmarks";
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

  // History Table
  public static final String    TBL_HISTORY         = "History";
  public static final String    COL_HIST_URI        = "histURI";
  public static final String    COL_VISITS          = "visits";
  public static final String    COL_TRANS_TYPE      = "transitionType";
  public static final String    COL_DATE_VISITED    = "dateVisited";
  
  protected AndroidBrowserRepositoryDatabaseHelper(Context context) {
    super(context, DB_NAME, null, SCHEMA_VERSION);
  }

  public void onCreate(SQLiteDatabase db) {
    String commonAllFields = COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
    + COL_GUID + " TEXT, " + COL_LAST_MOD + " INTEGER, " + COL_DELETED +
    " INTEGER DEFAULT 0";   
    String commonHistBookmarkFields = COL_ANDROID_ID + " INTEGER, " + COL_TITLE + " TEXT";
    
    String createBookmarksSql = "CREATE TABLE " + TBL_BOOKMARKS + " ("
        + commonAllFields + ", " + commonHistBookmarkFields + ", " + COL_BMK_URI
        + " TEXT, " + COL_DESCRIP + " TEXT, " + COL_LOAD_IN_SIDEBAR
        + " INTEGER, " + COL_TAGS + " TEXT, " + COL_KEYWORD + " TEXT, "
        + COL_PARENT_ID + " TEXT, " + COL_PARENT_NAME + " TEXT, " + COL_TYPE
        + " TEXT, " + COL_GENERATOR_URI + " TEXT, " + COL_STATIC_TITLE
        + " TEXT, " + COL_FOLDER_NAME + " TEXT, " + COL_QUERY_ID + " TEXT, "
        + COL_SITE_URI + " TEXT, " + COL_FEED_URI + " TEXT, " + COL_POS
        + " TEXT, " + COL_CHILDREN + " TEXT)"; 
    db.execSQL(createBookmarksSql);
    
    String createHistorySql = "CREATE TABLE " + TBL_HISTORY + " ("
        + commonAllFields + ", " + commonHistBookmarkFields + ", "
        + COL_HIST_URI + " TEXT, " + COL_VISITS + " TEXT, "
        + COL_TRANS_TYPE + " INTEGER, " + COL_DATE_VISITED
        + " INTEGER)";
    db.execSQL(createHistorySql);
  }

  // Cache these so we don't have to track them across cursors. Call `close`
  // when you're done.
  private static SQLiteDatabase readableDatabase;
  private static SQLiteDatabase writableDatabase;

  protected SQLiteDatabase getCachedReadableDatabase() {
    if (AndroidBrowserRepositoryDatabaseHelper.readableDatabase == null) {
      if (AndroidBrowserRepositoryDatabaseHelper.writableDatabase == null) {
        AndroidBrowserRepositoryDatabaseHelper.readableDatabase = this.getReadableDatabase();
        return AndroidBrowserRepositoryDatabaseHelper.readableDatabase;
      } else {
        return AndroidBrowserRepositoryDatabaseHelper.writableDatabase;
      }
    } else {
      return AndroidBrowserRepositoryDatabaseHelper.readableDatabase;
    }
  }

  protected SQLiteDatabase getCachedWritableDatabase() {
    if (AndroidBrowserRepositoryDatabaseHelper.writableDatabase == null) {
      AndroidBrowserRepositoryDatabaseHelper.writableDatabase = this.getWritableDatabase();
    }
    return AndroidBrowserRepositoryDatabaseHelper.writableDatabase;
  }

  public void close() {
    if (AndroidBrowserRepositoryDatabaseHelper.readableDatabase != null) {
      AndroidBrowserRepositoryDatabaseHelper.readableDatabase.close();
      AndroidBrowserRepositoryDatabaseHelper.readableDatabase = null;
    }
    if (AndroidBrowserRepositoryDatabaseHelper.writableDatabase != null) {
      AndroidBrowserRepositoryDatabaseHelper.writableDatabase.close();
      AndroidBrowserRepositoryDatabaseHelper.writableDatabase = null;
    }
    super.close();
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // For now we'll just drop and recreate the tables
    db.execSQL("DROP TABLE IF EXISTS " + TBL_BOOKMARKS);
    db.execSQL("DROP TABLE IF EXISTS " + TBL_HISTORY);
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
  
  public long insert(Record record) {
    SQLiteDatabase db = this.getCachedReadableDatabase();
    ContentValues cv = getContentValues(record);
    long rowId = db.insert(getTable(), null, cv);
    Log.i(TAG, "Inserted record into row: " + rowId);
    return rowId;
  }
  
  public Cursor fetchAllOrderByAndroidId() {
    SQLiteDatabase db = this.getCachedReadableDatabase();
    return db.query(getTable(), getAllColumns(), null, null, null, null, COL_ANDROID_ID);
  }
  
  public Cursor getGUIDSSince(long timestamp) {
    SQLiteDatabase db = this.getCachedReadableDatabase();
    return db.query(getTable(), new String[] {COL_GUID}, COL_LAST_MOD + " >= " +
        Long.toString(timestamp), null, null, null, null);
  }

  public Cursor fetchSince(long timestamp) {
    SQLiteDatabase db = this.getCachedReadableDatabase();
    return db.query(getTable(), getAllColumns(), COL_LAST_MOD + " >= " +
        Long.toString(timestamp), null, null, null, null);
  }
  
  public Cursor fetch(String guids[]) {
    SQLiteDatabase db = this.getCachedReadableDatabase();
    String where = COL_GUID + " in (";
    for (String guid : guids) {
      where = where + "'" + guid + "', ";
    }
    where = (where.substring(0, where.length() -2) + ")");
    String queryString = SQLiteQueryBuilder.buildQueryString(false, getTable(), getAllColumns(),
        where, null, null, null, null);
    return db.rawQuery(queryString, null);
  }
  
  public void delete(Record record) {
    SQLiteDatabase db = this.getCachedWritableDatabase();
    String[] where = new String[] { String.valueOf(record.guid) };
    db.delete(getTable(), COL_GUID+"=?", where);
  }
  
  public void updateByGuid(String guid, ContentValues cv) {
    SQLiteDatabase db = this.getCachedWritableDatabase();
    String[] where = new String[] { String.valueOf(guid) };
    db.update(getTable(), cv, COL_GUID + "=?", where);
  }

  protected abstract String getTable();
  protected abstract String[] getAllColumns();
  protected abstract ContentValues getContentValues(Record record);
  
}