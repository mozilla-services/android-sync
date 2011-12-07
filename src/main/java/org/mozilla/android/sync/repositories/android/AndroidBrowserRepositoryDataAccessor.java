package org.mozilla.android.sync.repositories.android;

import org.mozilla.android.sync.repositories.domain.Record;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public abstract class AndroidBrowserRepositoryDataAccessor {
  
  protected Context context;
  
  public AndroidBrowserRepositoryDataAccessor(Context context) {
    this.context = context;
  }
  
  public void wipe() {
    // TODO test to make sure this works, not confident in this
    context.getContentResolver().delete(getUri(), null, null);
  }
  
  public void purgeDeleted() {
    // TODO write tests for this
    Cursor cur = context.getContentResolver().query(getUri(),
        new String[] { BrowserContract.SyncColumns.GUID },
        BrowserContract.SyncColumns.DELETED + "= 1", null, null);
    cur.moveToFirst();
    while (!cur.isAfterLast()) {
      String guid = DBUtils.getStringFromCursor(cur, BrowserContract.SyncColumns.GUID);
      context.getContentResolver().delete(getUri(), BrowserContract.SyncColumns.GUID, new String[] { guid });
      cur.moveToNext();
    }
  }

  public Uri insert(Record record) {
    ContentValues cv = getContentValues(record);
    return context.getContentResolver().insert(getUri(), cv);
  }
  
  public Cursor fetchAll() {
    return context.getContentResolver().query(getUri(), null, null, null, null);
  }
  
  public Cursor getGUIDSSince(long timestamp) {
    return context.getContentResolver().query(getUri(), 
        new String[] {BrowserContract.SyncColumns.GUID},
        BrowserContract.SyncColumns.DATE_MODIFIED + " >= " +
        Long.toString(timestamp), null, null);
  }

  public Cursor fetchSince(long timestamp) {
    return context.getContentResolver().query(getUri(), null,
        BrowserContract.SyncColumns.DATE_MODIFIED + " >= " +
        Long.toString(timestamp), null, null);
  }
  
  public Cursor fetch(String guids[]) {
    String where = BrowserContract.SyncColumns.GUID + " in (";
    for (String guid : guids) {
      where = where + "'" + guid + "', ";
    }
    where = (where.substring(0, where.length() -2) + ")");
    // TODO this is a potential source of error, make sure this query works
    return context.getContentResolver().query(getUri(), null, where, null, null);
  }
  
  public void delete(Record record) {
    context.getContentResolver().delete(getUri(),
        BrowserContract.SyncColumns.GUID, new String[] { record.guid });
  }
  
  // TODO do we need a mark deleted and a deleted column. Probably not since
  // iirc this was for local sync
  
  public void updateByGuid(String guid, ContentValues cv) {
    context.getContentResolver().update(getUri(), cv,
        BrowserContract.SyncColumns.GUID, new String[] { guid});
  }
  
  protected abstract ContentValues getContentValues(Record record);
  protected abstract Uri getUri();

}
