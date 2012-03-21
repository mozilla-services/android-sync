package org.mozilla.gecko.sync.repositories.android;

import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class AndroidBrowserDeletedFormHistoryDataAccessor extends
    AndroidBrowserRepositoryDataAccessor {

  @SuppressWarnings("unused")
  private static final String LOG_TAG = "DeletedDataAccessor";

  public AndroidBrowserDeletedFormHistoryDataAccessor(Context context) {
    super(context);
  }

  @Override
  protected Uri getUri() {
    return BrowserContractHelpers.DELETED_FORM_HISTORY_CONTENT_URI;
  }

  @Override
  protected ContentValues getContentValues(Record record) {
    ContentValues cv = new ContentValues();
    cv.put(BrowserContract.DeletedColumns.GUID, record.guid);
    // BrowserContract.DeletedColumns.ID and TIME_DELETED set by content provider.
    return cv;
  }

  @Override
  protected String[] getAllColumns() {
    return BrowserContractHelpers.DeletedColumns;
  }

  @Override
  public String dateModifiedWhere(long timestamp) {
    return BrowserContract.DeletedFormHistory.TIME_DELETED + " >= " + Long.toString(timestamp); // Fennec works in microseconds.
  }

  @Override
  protected void addTimestampsForInsert(ContentValues values, Record record) {
    long now = System.currentTimeMillis();
    values.put(BrowserContract.DeletedFormHistory.TIME_DELETED, now);
  }

  @Override
  protected void addTimestampsForUpdate(ContentValues values, Record record) {
    long now = System.currentTimeMillis();
    values.put(BrowserContract.DeletedFormHistory.TIME_DELETED, now);
  }

  public Record updateRecordFromMirrorCursor(Record record, Cursor cur) {
    record.guid = RepoUtils.getStringFromCursor(cur, BrowserContract.DeletedFormHistory.GUID);
    record.androidID = RepoUtils.getLongFromCursor(cur, BrowserContract.DeletedFormHistory.ID);
    record.lastModified = RepoUtils.getLongFromCursor(cur, BrowserContract.DeletedFormHistory.TIME_DELETED);
    record.deleted = true; // We're definitely in the deleted table.

    return record; // XXX Logging record.log(LOG_TAG);
  }
}
