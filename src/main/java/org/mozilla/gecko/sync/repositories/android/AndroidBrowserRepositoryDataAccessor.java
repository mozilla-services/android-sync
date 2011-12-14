package org.mozilla.gecko.sync.repositories.android;

import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public abstract class AndroidBrowserRepositoryDataAccessor {

  protected Context context;
  protected String LOG_TAG = "AndroidBrowserRepositoryDataAccessor";

  public AndroidBrowserRepositoryDataAccessor(Context context) {
    this.context = context;
  }

  protected abstract String[] getAllColumns();
  protected abstract ContentValues getContentValues(Record record);
  protected abstract Uri getUri();
  protected long queryStart = 0;
  protected long queryEnd = 0;

  public void wipe() {
    Log.i(LOG_TAG, "wiping: " + getUri());
    String where = BrowserContract.SyncColumns.GUID + " NOT IN ('mobile')";
    context.getContentResolver().delete(getUri(), where, null);
  }

  public void purgeDeleted() throws NullCursorException {
    queryStart = System.currentTimeMillis();
    Uri uri = getUri().buildUpon().appendQueryParameter(BrowserContract.PARAM_SHOW_DELETED, "true").build();
    Cursor cur = context.getContentResolver().query(uri,
        new String[] { BrowserContract.SyncColumns.GUID },
        BrowserContract.SyncColumns.IS_DELETED + "= 1", null, null);
    queryEnd = System.currentTimeMillis();
    queryTimeLogger(LOG_TAG + ".purgeDeleted");
    if (cur == null) {
      Log.e(LOG_TAG, "Got back a null cursor in AndroidBrowserRepositoryDataAccessor.purgeDeleted");
      throw new NullCursorException(null);
    }
    cur.moveToFirst();
    while (!cur.isAfterLast()) {
      String guid = DBUtils.getStringFromCursor(cur, BrowserContract.SyncColumns.GUID);
      context.getContentResolver().delete(getUri(), BrowserContract.SyncColumns.GUID + " = '" + guid + "'", null);
      cur.moveToNext();
    }
    cur.close();
  }

  protected void queryTimeLogger(String methodCallingQuery) {
    long elapsedTime = queryEnd - queryStart;
    Log.i(LOG_TAG, "Query timer: " + methodCallingQuery + " took " + elapsedTime + "ms.");
  }

  public Uri insert(Record record) {
    ContentValues cv = getContentValues(record);
    return context.getContentResolver().insert(getUri(), cv);
  }

  public Cursor fetchAll() throws NullCursorException {
    queryStart = System.currentTimeMillis();
    Cursor cur = context.getContentResolver().query(getUri(),
        getAllColumns(), null, null, null);
    queryEnd = System.currentTimeMillis();

    queryTimeLogger(LOG_TAG + ".fetchAll");
    if (cur == null) {
      Log.e(LOG_TAG, "Got null cursor exception in AndroidBrowserRepositoryDataAccessor.fetchAll");
      throw new NullCursorException(null);
    }
    return cur;
  }

  public Cursor getGUIDsSince(long timestamp) throws NullCursorException {
    queryStart = System.currentTimeMillis();
    Cursor cur = context.getContentResolver().query(getUri(),
        new String[] { BrowserContract.SyncColumns.GUID },
        BrowserContract.SyncColumns.DATE_MODIFIED + " >= " +
        Long.toString(timestamp), null, null);
    queryEnd = System.currentTimeMillis();
    queryTimeLogger(LOG_TAG + ".getGUIDsSince");
    if (cur == null) {
      Log.e(LOG_TAG, "Got null cursor exception in AndroidBrowserRepositoryDataAccessor.getGUIDsSince");
      throw new NullCursorException(null);
    }
    return cur;
  }

  public Cursor fetchSince(long timestamp) throws NullCursorException {
    queryStart = System.currentTimeMillis();
    Cursor cur = context.getContentResolver().query(getUri(),
        getAllColumns(),
        BrowserContract.SyncColumns.DATE_MODIFIED + " >= " +
        Long.toString(timestamp), null, null);
    queryEnd = System.currentTimeMillis();
    queryTimeLogger(LOG_TAG + ".fetchSince");
    if (cur == null) {
      Log.e(LOG_TAG, "Got null cursor exception in AndroidBrowserRepositoryDataAccessor.fetchSince");
      throw new NullCursorException(null);
    }
    return cur;
  }

  public Cursor fetch(String guids[]) throws NullCursorException {
    String where = "guid" + " in (";
    for (String guid : guids) {
      where = where + "'" + guid + "', ";
    }
    where = (where.substring(0, where.length() -2) + ")");
    queryStart = System.currentTimeMillis();
    Cursor cur = context.getContentResolver().query(getUri(), getAllColumns(), where, null, null);
    queryEnd = System.currentTimeMillis();
    queryTimeLogger(LOG_TAG + ".fetch");
    if (cur == null) {
      Log.e(LOG_TAG, "Got null cursor exception in AndroidBrowserRepositoryDataAccessor.fetch");
      throw new NullCursorException(null);
    }
    return cur;
  }

  public void delete(Record record) {
    context.getContentResolver().delete(getUri(),
         BrowserContract.SyncColumns.GUID + " = '" + record.guid +"'", null);
  }

  public void updateByGuid(String guid, ContentValues cv) {
    context.getContentResolver().update(getUri(), cv,
        BrowserContract.SyncColumns.GUID + " = '" + guid +"'", null);
  }
}
