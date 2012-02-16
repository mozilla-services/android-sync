/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories.android;

import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.domain.ClientRecord;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class ClientsDatabase extends CachedSQLiteOpenHelper {

  public static final String LOG_TAG = "ClientsDatabase";

  // Database Specifications.
  protected static final String DB_NAME = "clients_database";
  protected static final int SCHEMA_VERSION = 1;

  // Clients Table.
  public static final String TBL_CLIENTS      = "clients";
  public static final String COL_ACCOUNT_GUID = "guid";
  public static final String COL_PROFILE      = "profile";
  public static final String COL_NAME         = "name";
  public static final String COL_TYPE         = "device_type";

  private final RepoUtils.QueryHelper queryHelper;

  public ClientsDatabase(Context context) {
    super(context, DB_NAME, null, SCHEMA_VERSION);
    this.queryHelper = new RepoUtils.QueryHelper(context, null, LOG_TAG);
  }

  @Override
  public void onCreate(SQLiteDatabase db) {
    String createTableSql = "CREATE TABLE " + TBL_CLIENTS + " ("
        + COL_ACCOUNT_GUID + " TEXT, "
        + COL_PROFILE + " TEXT, "
        + COL_NAME + " TEXT, "
        + COL_TYPE + " TEXT, "
        + "PRIMARY KEY (" + COL_ACCOUNT_GUID + ", " + COL_PROFILE + "))";
    db.execSQL(createTableSql);
  }

  public void wipe() {
    SQLiteDatabase db = this.getCachedWritableDatabase();
    onUpgrade(db, SCHEMA_VERSION, SCHEMA_VERSION);
  }

  @Override
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    // For now we'll just drop and recreate the tables.
    db.execSQL("DROP TABLE IF EXISTS " + TBL_CLIENTS);
    onCreate(db);
  }

  // If a record with given GUID exists, we'll delete it
  // and store the updated version.
  public long store(String accountGUID, ClientRecord record) {
    SQLiteDatabase db = this.getCachedReadableDatabase();

    // Delete if exists.
    delete(accountGUID, record.guid);

    // insert new
    ContentValues cv = new ContentValues();
    cv.put(COL_ACCOUNT_GUID, accountGUID);
    cv.put(COL_PROFILE, record.guid);
    cv.put(COL_NAME, record.name);
    cv.put(COL_TYPE, record.type);

    long rowId = db.insert(TBL_CLIENTS, null, cv);
    Log.i(LOG_TAG, "Inserted client record into row: " + rowId);
    return rowId;
  }

  public Cursor fetch(String accountGuid, String profileId) throws NullCursorException {
    String[] columns = new String[] { COL_ACCOUNT_GUID, COL_PROFILE, COL_NAME, COL_TYPE };
    String where = COL_ACCOUNT_GUID + " = ? and " + COL_PROFILE + " = ?";
    String[] args = new String[] { accountGuid, profileId };

    SQLiteDatabase db = this.getCachedReadableDatabase();

    Cursor cur = queryHelper.safeQuery(db, ".fetch", TBL_CLIENTS, columns, where, args, null, null, null, null);
    return cur;
  }

  public Cursor fetchAll() throws NullCursorException {
    String[] columns = new String[] { COL_ACCOUNT_GUID, COL_PROFILE, COL_NAME, COL_TYPE };
    SQLiteDatabase db = this.getCachedReadableDatabase();

    Cursor cur = queryHelper.safeQuery(db, ".fetch", TBL_CLIENTS, columns, null, null, null, null, null, null);
    return cur;
  }

  public void delete(String accountGuid, String profileId) {
    String where = COL_ACCOUNT_GUID + " = ? and " + COL_PROFILE + " = ?";
    String[] args = new String[] { accountGuid, profileId };

    SQLiteDatabase db = this.getCachedWritableDatabase();
    db.delete(TBL_CLIENTS, where, args);
  }
}
