package org.mozilla.gecko.sync.synchronizer.managers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.mozilla.gecko.sync.repositories.android.CachedSQLiteOpenHelper;
import org.mozilla.gecko.sync.repositories.android.RepoUtils;
import org.mozilla.gecko.sync.synchronizer.FillingGuidsManager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class SQLiteGuidsManager extends CachedSQLiteOpenHelper implements FillingGuidsManager {

  public static final String LOG_TAG = "SQLiteGuidsMan";

  // Database Specifications.
  protected static final String DB_NAME = "filling_database";
  protected static final int SCHEMA_VERSION = 1;

  // Clients Table.
  public static final String TBL = "guids";
  public static final String COL_COLLECTION = "collection";
  public static final String COL_MODIFIED = "modified";
  public static final String COL_GUID = "guid";
  public static final String COL_RETRIES_REMAINING = "retries_remaining";

  public static final String[] TBL_COLUMNS = new String[] { COL_COLLECTION, COL_MODIFIED, COL_GUID, COL_RETRIES_REMAINING };

  protected final String collection;
  protected final int batchSize;
  protected final int numRetries;

  public SQLiteGuidsManager(Context context, final String collection, final int batchSize, final int numRetries) {
    super(context, DB_NAME, null, SCHEMA_VERSION);
    this.collection = collection;
    this.batchSize = batchSize;
    this.numRetries = numRetries;
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    String createTableSql = "CREATE TABLE " + TBL + " ("
        + COL_COLLECTION + " TEXT NOT NULL, "
        + COL_MODIFIED + " INTEGER NOT NULL, "
        + COL_GUID + " TEXT NOT NULL, "
        + COL_RETRIES_REMAINING + " INTEGER NOT NULL"
        + ", PRIMARY KEY (" + COL_COLLECTION + ", " + COL_GUID + ")"
        + ")";
    db.execSQL(createTableSql);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // For now we'll just drop and recreate the tables.
    db.execSQL("DROP TABLE IF EXISTS " + TBL);
    onCreate(db);
  }

  public void wipeDB() {
    SQLiteDatabase db = this.getCachedWritableDatabase();
    onUpgrade(db, SCHEMA_VERSION, SCHEMA_VERSION);
  }

  public void wipeTable() {
    SQLiteDatabase db = this.getCachedWritableDatabase();
    db.execSQL("DELETE FROM " + TBL);
  }

  @Override
  public void addFreshGuids(Collection<String> guids) throws Exception {
    if (guids.isEmpty()) {
      return;
    }
    SQLiteDatabase db = this.getCachedWritableDatabase();
    try{
      db.beginTransaction();
      ContentValues cv = new ContentValues();
      cv.put(COL_COLLECTION, this.collection);
      cv.put(COL_RETRIES_REMAINING, this.numRetries);
      cv.put(COL_MODIFIED, System.currentTimeMillis());
      final String where = COL_GUID + " = ? AND " + COL_COLLECTION + " = ?";
      for (String guid : guids) {
        cv.put(COL_GUID, guid);
        if (db.update(TBL, cv, where, new String[] { guid, this.collection }) < 1) {
          db.insert(TBL, null, cv);
        }
      }
      db.setTransactionSuccessful();
    } finally {
      db.endTransaction();
    }
  }

  @Override
  public List<String> nextGuids() throws Exception {
    SQLiteDatabase db = this.getCachedReadableDatabase();
    List<String> guids = new ArrayList<String>();
    Cursor cursor = null;
    try {
      cursor = db.rawQuery("SELECT " + COL_GUID + " FROM " + TBL + " WHERE collection = ? ORDER BY " + COL_MODIFIED + " DESC, ROWID DESC LIMIT ? ",
          new String[] { this.collection, Integer.toString(this.batchSize) });
      if (!cursor.moveToFirst()) {
        return guids;
      }
      int pos = cursor.getColumnIndexOrThrow(COL_GUID);
      while (!cursor.isAfterLast()) {
        guids.add(cursor.getString(pos));
        cursor.moveToNext();
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return guids;
  }

  @Override
  public int numGuidsRemaining() {
    SQLiteDatabase db = this.getCachedReadableDatabase();
    Cursor cursor = null;
    try {
      cursor = db.rawQuery("SELECT COUNT(*) FROM " + TBL + " WHERE collection = ?",
          new String[] { this.collection });
      if (!cursor.moveToFirst()) {
        return 0;
      }
      return cursor.getInt(0);
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
  }

  @Override
  public void removeGuids(Collection<String> guids) throws Exception {
    SQLiteDatabase db = this.getCachedWritableDatabase();
    final String where = "(" + RepoUtils.computeSQLInClause(guids.size(), COL_GUID) + ") AND " + COL_COLLECTION + " = ?";
    final String guidsArray[] = guids.toArray(new String[guids.size()+1]);
    guidsArray[guids.size()] = this.collection;
    db.delete(TBL, where, guidsArray);
  }

  @Override
  public void retryGuids(Collection<String> guids) throws Exception {
    SQLiteDatabase db = this.getCachedWritableDatabase();
    final String where = "(" + RepoUtils.computeSQLInClause(guids.size(), COL_GUID) + ") AND " + COL_COLLECTION + " = ?";
    final String guidsArray[] = guids.toArray(new String[guids.size()+1]);
    guidsArray[guids.size()] = this.collection;
    db.execSQL("UPDATE " + TBL + " SET " + COL_RETRIES_REMAINING + " = " + COL_RETRIES_REMAINING + " - 1 WHERE " + where, guidsArray);
    db.delete(TBL, COL_RETRIES_REMAINING + " <= 0", null);
  }
}
