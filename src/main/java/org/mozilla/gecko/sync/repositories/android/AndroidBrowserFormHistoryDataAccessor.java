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
  protected ContentValues getContentValuesForInsert(Record record) {
    ContentValues cv = getContentValuesForUpdate(record);
    long now = System.currentTimeMillis();
    cv.put(BrowserContract.FormHistory.FIRST_USED, now * 1000); // Microseconds.
    cv.put(BrowserContract.FormHistory.LAST_USED,  now * 1000); // Microseconds.

    return cv;
  }

  @Override
  protected ContentValues getContentValuesForUpdate(Record record) {
    ContentValues cv = new ContentValues();
    FormHistoryRecord rec = (FormHistoryRecord) record;
    long now = System.currentTimeMillis();
    cv.put(BrowserContract.FormHistory.GUID,       rec.guid);
    cv.put(BrowserContract.FormHistory.FIELD_NAME, rec.fieldName);
    cv.put(BrowserContract.FormHistory.VALUE,      rec.fieldValue);
    cv.put(BrowserContract.FormHistory.LAST_USED,  now * 1000); // Microseconds.

    return cv;
  }

  @Override
  public String dateModifiedWhere(long timestamp) {
    return BrowserContract.FormHistory.FIRST_USED + " >= " + Long.toString(1000 * timestamp); // Fennec works in microseconds.
  }

  // Create a FormHistoryRecord object from a cursor on a row with a moz_formhistory record in it
  public FormHistoryRecord formHistoryFromMirrorCursor(Cursor cur) {
    String guid = RepoUtils.getStringFromCursor(cur, BrowserContract.FormHistory.GUID);
    String collection = "formhistory";
    FormHistoryRecord rec = new FormHistoryRecord(guid, collection, 0, false);

    rec.fieldName = RepoUtils.getStringFromCursor(cur, BrowserContract.FormHistory.FIELD_NAME);
    rec.fieldValue = RepoUtils.getStringFromCursor(cur, BrowserContract.FormHistory.VALUE);
    rec.androidID = RepoUtils.getLongFromCursor(cur, BrowserContract.FormHistory.ID);
    rec.lastModified = RepoUtils.getLongFromCursor(cur, BrowserContract.FormHistory.FIRST_USED) / 1000; // Convert microseconds to milliseconds.
    rec.deleted = false; // If we're looking at a moz_formhistory record, we're not in the deleted table.

    rec.log(LOG_TAG);
    return rec;
  }
}
