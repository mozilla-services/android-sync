/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories.android;

import org.json.simple.JSONArray;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.repositories.NullCursorException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class AndroidBrowserHistoryDataExtender extends SQLiteOpenHelper {

  public static final String LOG_TAG = "SyncHistoryVisits";

  // Database Specifications.
  protected static final String DB_NAME = "history_extension_database";
  protected static final int SCHEMA_VERSION = 1;

  // History Table.
  public static final String   TBL_HISTORY_EXT = "HistoryExtension";
  public static final String   COL_GUID = "guid";
  public static final String   COL_VISITS = "visits";
  public static final String[] TBL_COLUMNS = { COL_GUID, COL_VISITS };

  private final RepoUtils.QueryHelper queryHelper;

  public AndroidBrowserHistoryDataExtender(Context context) {
    super(context, DB_NAME, null, SCHEMA_VERSION);
    this.queryHelper = new RepoUtils.QueryHelper(context, null, LOG_TAG);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    String createTableSql = "CREATE TABLE " + TBL_HISTORY_EXT + " ("
        + COL_GUID + " TEXT PRIMARY KEY, "
        + COL_VISITS + " TEXT)";
    db.execSQL(createTableSql);
  }

  // Cache these so we don't have to track them across cursors. Call `close`
  // when you're done.
  private static SQLiteDatabase readableDatabase;
  private static SQLiteDatabase writableDatabase;

  protected SQLiteDatabase getCachedReadableDatabase() {
    if (AndroidBrowserHistoryDataExtender.readableDatabase == null) {
      if (AndroidBrowserHistoryDataExtender.writableDatabase == null) {
        AndroidBrowserHistoryDataExtender.readableDatabase = this.getReadableDatabase();
        return AndroidBrowserHistoryDataExtender.readableDatabase;
      } else {
        return AndroidBrowserHistoryDataExtender.writableDatabase;
      }
    } else {
      return AndroidBrowserHistoryDataExtender.readableDatabase;
    }
  }

  protected SQLiteDatabase getCachedWritableDatabase() {
    if (AndroidBrowserHistoryDataExtender.writableDatabase == null) {
      AndroidBrowserHistoryDataExtender.writableDatabase = this.getWritableDatabase();
    }
    return AndroidBrowserHistoryDataExtender.writableDatabase;
  }

  @Override
  public void close() {
    if (AndroidBrowserHistoryDataExtender.readableDatabase != null) {
      AndroidBrowserHistoryDataExtender.readableDatabase.close();
      AndroidBrowserHistoryDataExtender.readableDatabase = null;
    }
    if (AndroidBrowserHistoryDataExtender.writableDatabase != null) {
      AndroidBrowserHistoryDataExtender.writableDatabase.close();
      AndroidBrowserHistoryDataExtender.writableDatabase = null;
    }
    super.close();
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // For now we'll just drop and recreate the tables.
    db.execSQL("DROP TABLE IF EXISTS " + TBL_HISTORY_EXT);
    onCreate(db);
  }

  public void wipe() {
    SQLiteDatabase db = this.getCachedWritableDatabase();
    onUpgrade(db, SCHEMA_VERSION, SCHEMA_VERSION);
  }

  /**
   * Store visit data.
   *
   * If a row with GUID `guid` does not exist, insert a new row.
   * If a row with GUID `guid` does exist, replace the visits column.
   *
   * @param guid The GUID to store to.
   * @param visits New visits data.
   */
  public void store(String guid, JSONArray visits) {
    SQLiteDatabase db = this.getCachedWritableDatabase();

    ContentValues cv = new ContentValues();
    cv.put(COL_GUID, guid);
    if (visits == null) {
      cv.put(COL_VISITS, "[]");
    } else {
      cv.put(COL_VISITS, visits.toJSONString());
    }

    String where = COL_GUID + " = ?";
    String[] args = new String[] { guid };
    int rowsUpdated = db.update(TBL_HISTORY_EXT, cv, where, args);
    if (rowsUpdated >= 1) {
      Logger.debug(LOG_TAG, "Replaced history extension record for row with GUID " + guid);
    } else {
      long rowId = db.insert(TBL_HISTORY_EXT, null, cv);
      Logger.debug(LOG_TAG, "Inserted history extension record into row: " + rowId);
    }
  }

  /**
   * Fetch a row.
   *
   * @param guid The GUID of the row to fetch.
   * @return A Cursor.
   * @throws NullCursorException
   */
  public Cursor fetch(String guid) throws NullCursorException {
    String where = COL_GUID + " = ?";
    String[] args = new String[] { guid };

    SQLiteDatabase db = this.getCachedReadableDatabase();
    Cursor cur = queryHelper.safeQuery(db, ".fetch", TBL_HISTORY_EXT,
        TBL_COLUMNS,
        where, args, null, null, null, null);
    return cur;
  }

  public JSONArray visitsForGUID(String guid) throws NullCursorException {
    Logger.debug(LOG_TAG, "Fetching visits for GUID " + guid);
    Cursor visits = fetch(guid);
    try {
      if (!visits.moveToFirst()) {
        // Cursor is empty.
        return new JSONArray();
      } else {
        return RepoUtils.getJSONArrayFromCursor(visits, COL_VISITS);
      }
    } finally {
      visits.close();
    }
  }

  /**
   * Delete a row.
   *
   * @param guid The GUID of the row to delete.
   * @return The number of rows deleted, either 0 (if a row with this GUID does not exist) or 1.
   */
  public int delete(String guid) {
    String where = COL_GUID + " = ?";
    String[] args = new String[] { guid };

    SQLiteDatabase db = this.getCachedWritableDatabase();
    return db.delete(TBL_HISTORY_EXT, where, args);
  }

  /**
   * Fetch all rows.
   *
   * @return A Cursor.
   * @throws NullCursorException
   */
  public Cursor fetchAll() throws NullCursorException {
    SQLiteDatabase db = this.getCachedReadableDatabase();
    Cursor cur = db.query(TBL_HISTORY_EXT,
        TBL_COLUMNS,
        null, null, null, null, null);
    if (cur == null) {
      throw new NullCursorException(null);
    }
    return cur;
  }
}
