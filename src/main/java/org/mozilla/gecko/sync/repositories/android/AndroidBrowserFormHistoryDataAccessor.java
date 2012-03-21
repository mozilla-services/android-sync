/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories.android;

import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.sync.repositories.domain.FormHistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

public class AndroidBrowserFormHistoryDataAccessor extends
    AndroidBrowserRepositoryDataAccessor {

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
  public void purgeDeleted() {
    // Do nothing, since this table holds no deleted records.
  }

  @Override
  protected ContentValues getContentValues(Record record) {
    ContentValues cv = new ContentValues();
    FormHistoryRecord rec = (FormHistoryRecord) record;
    cv.put(BrowserContract.SyncColumns.GUID,       rec.guid);
    // cv.put(BrowserContract.FormHistory.ID,         rec.id);
    cv.put(BrowserContract.FormHistory.FIELD_NAME, rec.fieldName);
    cv.put(BrowserContract.FormHistory.VALUE,      rec.fieldValue);
    /*
    cv.put(BrowserContract.FormHistory.TIMES_USED, rec.fennecTimesUsed);
    cv.put(BrowserContract.FormHistory.FIRST_USED, rec.fennecFirstUsed);
    cv.put(BrowserContract.FormHistory.LAST_USED,  rec.fennecLastUsed);
     */
    return cv;
  }

  @Override
  public String dateModifiedWhere(long timestamp) {
    return BrowserContract.FormHistory.FIRST_USED + " >= " + Long.toString(1000 * timestamp); // Fennec works in microseconds.
  }

  @Override
  protected void addTimestampsForInsert(ContentValues values, Record record) {
    long now = System.currentTimeMillis();
    values.put(BrowserContract.FormHistory.FIRST_USED, 1000 * now); // Fennec works in microseconds.
    values.put(BrowserContract.FormHistory.LAST_USED,  1000 * now);
  }

  @Override
  protected void addTimestampsForUpdate(ContentValues values, Record record) {
    long now = System.currentTimeMillis();
    values.put(BrowserContract.FormHistory.LAST_USED,  1000 * now); // Fennec works in microseconds.
  }

  // Create a FormHistoryRecord object from a cursor on a row with a moz_formhistory record in it
  public FormHistoryRecord formHistoryFromMirrorCursor(Cursor cur) {
    String guid = RepoUtils.getStringFromCursor(cur, BrowserContract.SyncColumns.GUID);
    String collection = "formhistory";
    // long lastModified = getLongFromCursor(cur, BrowserContract.SyncColumns.DATE_MODIFIED);
    // boolean deleted = getLongFromCursor(cur, BrowserContract.SyncColumns.IS_DELETED) == 1 ? true : false;
    FormHistoryRecord rec = new FormHistoryRecord(guid, collection, 0, false);

    rec.fieldName = RepoUtils.getStringFromCursor(cur, BrowserContract.FormHistory.FIELD_NAME);
    rec.fieldValue = RepoUtils.getStringFromCursor(cur, BrowserContract.FormHistory.VALUE);
    rec.androidID = RepoUtils.getLongFromCursor(cur, BrowserContract.FormHistory.ID);
    rec.lastModified = RepoUtils.getLongFromCursor(cur, BrowserContract.FormHistory.FIRST_USED) / 1000; // Convert microseconds to milliseconds.
    rec.deleted = false; // If we're looking at a moz_formhistory record, we're not in the deleted table.

    return logFormHistory(rec);
  }

  public FormHistoryRecord logFormHistory(FormHistoryRecord record) {
    record.log(LOG_TAG);
    return record;
  }
}
