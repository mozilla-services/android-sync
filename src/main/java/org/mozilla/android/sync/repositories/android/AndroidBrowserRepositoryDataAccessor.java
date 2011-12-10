package org.mozilla.android.sync.repositories.android;

import org.mozilla.android.sync.repositories.domain.Record;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public abstract class AndroidBrowserRepositoryDataAccessor {
  
  protected Context context;
  protected String tag = "AndroidBrowserRepositoryDataAccessor";
  
  public AndroidBrowserRepositoryDataAccessor(Context context) {
    this.context = context;
  }
  
  //protected abstract String[] getAllColumns();
  protected abstract ContentValues getContentValues(Record record);
  protected abstract Uri getUri();
  // TODO find a cleaner way of doing this 
  // once we are sure that qualifying columns
  // is necessary and mobile team isn't changing their
  // content provider.
  protected abstract String getGuidColumn();
  protected abstract String getDateModifiedColumn();
  protected abstract String getDeletedColumn();
  protected abstract String getAndroidIDColumn();
  
  public void wipe() {
    // TODO test to make sure this works, not confident in this
    Log.i("wipe", "wiping: " + getUri());
    context.getContentResolver().delete(getUri(), "", null);
  }
  
  public void purgeDeleted() {
    // TODO write tests for this
    Cursor cur = context.getContentResolver().query(getUri(),
        new String[] { getGuidColumn() },
        getDeletedColumn() + "= 1", null, null);
    cur.moveToFirst();
    while (!cur.isAfterLast()) {
      String guid = DBUtils.getStringFromCursor(cur, getGuidColumn());
      context.getContentResolver().delete(getUri(), getGuidColumn(), new String[] { guid });
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
        null,
        getDateModifiedColumn() + " >= " +
        Long.toString(timestamp), null, null);
  }

  public Cursor fetchSince(long timestamp) {
    return context.getContentResolver().query(getUri(), null,
        getDateModifiedColumn() + " >= " +
        Long.toString(timestamp), null, null);
  }
  
  public Cursor fetch(String guids[]) {
    String where = getGuidColumn() + " in (";
    for (String guid : guids) {
      where = where + "'" + guid + "', ";
    }
    where = (where.substring(0, where.length() -2) + ")");
    // TODO this is a potential source of error, make sure this query works
    return context.getContentResolver().query(getUri(), null, where, null, null);
  }
  
  public Cursor fetch(long androidID) {
    return context.getContentResolver().query(getUri(), null, 
        getAndroidIDColumn() + " = " + Long.toString(androidID), null, null);
  }
  
  public void delete(Record record) {
    context.getContentResolver().delete(getUri(),
        getGuidColumn(), new String[] { record.guid });
  }
  
  // TODO do we need a mark deleted and a deleted column. Probably not since
  // iirc this was for local sync
  
  public void updateByGuid(String guid, ContentValues cv) {
    context.getContentResolver().update(getUri(), cv,
        getGuidColumn(), new String[] { guid});
  }
  

}
