package org.mozilla.gecko.sync.repositories.android;

import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.sync.repositories.domain.FormHistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class AndroidBrowserDeletedFormHistoryDataAccessor extends
    AndroidBrowserRepositoryDataAccessor {
  private static final String LOG_TAG = "DeletedDataAccessor";

  public AndroidBrowserDeletedFormHistoryDataAccessor(Context context) {
    super(context);
  }

  @Override
  protected Uri getUri() {
    return BrowserContractHelpers.DELETED_FORM_HISTORY_CONTENT_URI;
  }

  @Override
  protected ContentValues getContentValuesForInsert(Record record) {
    return getContentValuesForUpdate(record);
  }

  @Override
  protected ContentValues getContentValuesForUpdate(Record record) {
    long now = System.currentTimeMillis();
    ContentValues cv = new ContentValues();
    cv.put(BrowserContract.DeletedFormHistory.GUID, record.guid);
    cv.put(BrowserContract.DeletedFormHistory.TIME_DELETED, now); // Milliseconds.

    return cv;
  }

  @Override
  protected String[] getAllColumns() {
    return BrowserContractHelpers.DeletedColumns;
  }

  @Override
  public String dateModifiedWhere(long timestamp) {
    return BrowserContract.DeletedFormHistory.TIME_DELETED + " >= " + Long.toString(timestamp);
  }

  public FormHistoryRecord formHistoryFromMirrorCursor(Cursor cur) {
    String guid = RepoUtils.getStringFromCursor(cur, BrowserContract.DeletedFormHistory.GUID);
    String collection = "formhistory";
    FormHistoryRecord rec = new FormHistoryRecord(guid, collection, 0, false);

    rec.guid = RepoUtils.getStringFromCursor(cur, BrowserContract.DeletedFormHistory.GUID);
    rec.androidID = RepoUtils.getLongFromCursor(cur, BrowserContract.DeletedFormHistory.ID);
    rec.lastModified = RepoUtils.getLongFromCursor(cur, BrowserContract.DeletedFormHistory.TIME_DELETED);
    rec.deleted = true; // We're definitely in the deleted table.

    rec.log(LOG_TAG);
    return rec;
  }
}
