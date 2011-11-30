package org.mozilla.android.sync.repositories;

import org.mozilla.android.sync.repositories.domain.Record;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

public interface RepositoryDatabaseHelper {
  
  public static final String COL_GUID = "guid";
  
  public void onCreate(SQLiteDatabase db);
  public void close();
  public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion);
  public void wipe() throws SQLiteException;
  public long insert(Record record);
  public Cursor fetchAllOrderByAndroidId();
  public Cursor getGUIDSSince(long timestamp);
  public Cursor fetchSince(long timestamp);
  public Cursor fetch(String guids[]);
  public void markDeleted(String guid);
  public void delete(Record record);
}
