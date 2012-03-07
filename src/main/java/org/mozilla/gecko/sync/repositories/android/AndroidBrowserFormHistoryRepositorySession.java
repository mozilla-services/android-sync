/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories.android;

import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.Repository;
import org.mozilla.gecko.sync.repositories.domain.FormHistoryRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.Context;
import android.database.Cursor;

public class AndroidBrowserFormHistoryRepositorySession extends AndroidBrowserRepositorySession {

  public AndroidBrowserFormHistoryDataAccessor regularAccessor;
  public AndroidBrowserDeletedFormHistoryDataAccessor deletedAccessor;

  public AndroidBrowserFormHistoryRepositorySession(Repository repository, Context context) {
    super(repository);
    regularAccessor = new AndroidBrowserFormHistoryDataAccessor(context);
    deletedAccessor = new AndroidBrowserDeletedFormHistoryDataAccessor(context);
    dbHelper = new AndroidBrowserMergeDataAccessor(context, regularAccessor, deletedAccessor);
  }

  @Override
  protected Record retrieveDuringStore(Cursor cur) {
    return retrieveDuringFetch(cur);
  }

  @Override
  protected Record retrieveDuringFetch(Cursor cur) {
    if (cur.getColumnCount() == BrowserContractHelpers.DeletedColumns.length) {
      return deletedAccessor.formHistoryFromMirrorCursor(cur);
    }

    return regularAccessor.formHistoryFromMirrorCursor(cur);
  }

  @Override
  protected String buildRecordString(Record record) {
    FormHistoryRecord formHistoryRecord = (FormHistoryRecord) record;
    return formHistoryRecord.fieldName + formHistoryRecord.fieldValue;
  }

  @Override
  protected Record transformRecord(Record record) throws NullCursorException {
    return record;
  }

  @Override
  protected Record prepareRecord(Record record) {
    return record;
  }
}
