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

  public AndroidBrowserFormHistoryRepositorySession(Repository repository, Context context) {
    super(repository);
    dbHelper = new AndroidBrowserFormHistoryDataAccessor(context);
    dbHelper.dumpDB();
  }

  @Override
  protected Record retrieveDuringStore(Cursor cur) {
    return ((AndroidBrowserFormHistoryDataAccessor)dbHelper).formHistoryFromMirrorCursor(cur);
  }

  @Override
  protected Record retrieveDuringFetch(Cursor cur) {
    return ((AndroidBrowserFormHistoryDataAccessor)dbHelper).formHistoryFromMirrorCursor(cur);
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
