package org.mozilla.gecko.sync.repositories.android;

import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.sync.repositories.domain.FormHistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;

public class AndroidBrowserFormHistoryDataAccessor extends
    AndroidBrowserRepositoryDataAccessor {

  @SuppressWarnings("unused")
  private static final String LOG_TAG = "FormHistoryDataAccessor";

  public AndroidBrowserFormHistoryDataAccessor(Context context) {
    super(context);
  }

  @Override
  protected Uri getUri() {
    return BrowserContractHelpers.FORM_HISTORY_CONTENT_URI;
  }

  @Override
  protected String[] getAllColumns() {
    return BrowserContractHelpers.FormHistoryColumns;
  }

  @Override
  protected ContentValues getContentValues(Record record) {
    ContentValues cv = new ContentValues();
    FormHistoryRecord rec = (FormHistoryRecord) record;
    cv.put(BrowserContract.SyncColumns.GUID,       rec.guid);
    // cv.put(BrowserContract.FormHistory.ID,         rec.id);
    cv.put(BrowserContract.FormHistory.FIELD_NAME, rec.fieldName);
    cv.put(BrowserContract.FormHistory.VALUE,      rec.fieldValue);

    cv.put(BrowserContract.FormHistory.TIMES_USED, rec.fennecTimesUsed);
    cv.put(BrowserContract.FormHistory.FIRST_USED, rec.fennecFirstUsed);
    cv.put(BrowserContract.FormHistory.LAST_USED,  rec.fennecLastUsed);

    // Note that we don't set the modified timestamp: we allow the
    // content provider to do that for us.
    return cv;
  }

  @Override
  public String dateModifiedWhere(long timestamp) {
    return BrowserContract.FormHistory.FIRST_USED + " >= " + Long.toString(timestamp);
  }
}
