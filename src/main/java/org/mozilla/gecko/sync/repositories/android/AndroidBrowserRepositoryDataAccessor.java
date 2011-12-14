package org.mozilla.gecko.sync.repositories.android;

import org.mozilla.gecko.sync.repositories.domain.Record;
import org.mozilla.gecko.sync.repositories.NullCursorException;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.Browser.BookmarkColumns;
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
  protected long queryStart = 0;
  protected long queryEnd = 0;
  // TODO find a cleaner way of doing this
  // once we are sure that qualifying columns
  // is necessary and mobile team isn't changing their
  // content provider.
  protected abstract String getGuidColumn();
  protected abstract String getDateModifiedColumn();
  protected abstract String getDeletedColumn();
  protected abstract String getAndroidIDColumn();

  public void wipe() {
    Log.i("wipe", "wiping: " + getUri());
    // TODO once I can actually delete properly, change this to
    // only not delete the mobile folder
    String where = BrowserContract.SyncColumns.GUID + " NOT IN ('mobile', 'menu', 'places', 'toolbar', 'unfiled')";
    context.getContentResolver().delete(getUri(), where, null);
  }
  
  public void purgeDeleted() throws NullCursorException {
    // TODO write tests for this
    queryStart = System.currentTimeMillis();
    Uri uri = getUri().buildUpon().appendQueryParameter(BrowserContract.PARAM_SHOW_DELETED, "true").build();
    Cursor cur = context.getContentResolver().query(uri,
        null, BrowserContract.SyncColumns.IS_DELETED + "= 1", null, null);
    queryEnd = System.currentTimeMillis();
    queryTimeLogger(tag + ".purgeDeleted");
    if (cur == null) {
      Log.e(tag, "Got back a null cursor in AndroidBrowserRepositoryDataAccessor.purgeDeleted");
      throw new NullCursorException(null);
    }
    cur.moveToFirst();
    while (!cur.isAfterLast()) {
      String guid = DBUtils.getStringFromCursor(cur, getGuidColumn());
      context.getContentResolver().delete(getUri(), BrowserContract.SyncColumns.GUID + " = '" + guid + "'", null);
      cur.moveToNext();
    }
    cur.close();
  }
  
  protected void queryTimeLogger(String methodCallingQuery) {
    long elapsedTime = queryEnd - queryStart;
    Log.i(tag, "Query timer: " + methodCallingQuery + " took " + elapsedTime + "ms.");
  }

  public Uri insert(Record record) {
    ContentValues cv = getContentValues(record);
    return context.getContentResolver().insert(getUri(), cv);
  }
  
  public Cursor fetchAll() throws NullCursorException {
    queryStart = System.currentTimeMillis();
    Cursor cur = context.getContentResolver().query(getUri(), BrowserContract.Bookmarks.BookmarksColumns, null, null, null);
    queryEnd = System.currentTimeMillis();
    
    queryTimeLogger(tag + ".fetchAll");
    if (cur == null) {
      Log.e(tag, "Got null cursor exception in AndroidBrowserRepositoryDataAccessor.fetchAll");
      throw new NullCursorException(null);
    }
    return cur;
  }
  
  public Cursor getGUIDsSince(long timestamp) throws NullCursorException {
    queryStart = System.currentTimeMillis();
    Cursor cur = context.getContentResolver().query(getUri(),
        null,
        getDateModifiedColumn() + " >= " +
        Long.toString(timestamp), null, null);
    queryEnd = System.currentTimeMillis();
    queryTimeLogger(tag + ".getGUIDsSince");
    if (cur == null) {
      Log.e(tag, "Got null cursor exception in AndroidBrowserRepositoryDataAccessor.getGUIDsSince");
      throw new NullCursorException(null);
    }
    return cur;
  }

  public Cursor fetchSince(long timestamp) throws NullCursorException {
    queryStart = System.currentTimeMillis();
    Cursor cur = context.getContentResolver().query(getUri(), null,
        getDateModifiedColumn() + " >= " +
        Long.toString(timestamp), null, null);
    queryEnd = System.currentTimeMillis();
    queryTimeLogger(tag + ".fetchSince");
    if (cur == null) {
      Log.e(tag, "Got null cursor exception in AndroidBrowserRepositoryDataAccessor.fetchSince");
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
    Cursor cur = context.getContentResolver().query(getUri(), BrowserContract.Bookmarks.BookmarksColumns, where, null, null);
    queryEnd = System.currentTimeMillis();
    queryTimeLogger(tag + ".fetch");
    if (cur == null) {
      Log.e(tag, "Got null cursor exception in AndroidBrowserRepositoryDataAccessor.fetch");
      throw new NullCursorException(null);
    }
    return cur;
  }

  public void delete(Record record) {
    context.getContentResolver().delete(getUri(),
        "guid = '" + record.guid +"'", null);
  }

  public void updateByGuid(String guid, ContentValues cv) {
    context.getContentResolver().update(getUri(), cv,
        "guid = '" + guid +"'", null);
  }
}
