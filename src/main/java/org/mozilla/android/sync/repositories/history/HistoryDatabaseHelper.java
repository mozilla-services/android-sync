package org.mozilla.android.sync.repositories.history;

import org.mozilla.android.sync.repositories.RepositoryDatabaseHelper;
import org.mozilla.android.sync.repositories.domain.Record;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;

public class HistoryDatabaseHelper extends RepositoryDatabaseHelper {

  public HistoryDatabaseHelper(Context context) {
    super(context);
  }

  @Override
  public void wipe() throws SQLiteException {
    // TODO Auto-generated method stub
    
  }

  @Override
  public long insert(Record record) {
    // TODO Auto-generated method stub
    return 0;
  }

  @Override
  public Cursor fetchAllOrderByAndroidId() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Cursor getGUIDSSince(long timestamp) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Cursor fetchSince(long timestamp) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public Cursor fetch(String[] guids) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void markDeleted(String guid) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void delete(Record record) {
    // TODO Auto-generated method stub
    
  }

  @Override
  public void updateByGuid(String guid, ContentValues cv) {
    // TODO Auto-generated method stub
    
  }

}
