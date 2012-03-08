/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories.android;

import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.sync.Logger;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.android.RepoUtils.QueryHelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

/**
 * An AndroidBrowserRepositoryDataAccessor that manages tracking deleted records in a separate table.
 */
public abstract class AndroidBrowserDeletedTableDataAccessor extends
    AndroidBrowserRepositoryDataAccessor {

  protected String LOG_TAG() {
    return "DelTableDataAccessor";
  }

  QueryHelper deletedQueryHelper;
  public AndroidBrowserDeletedTableDataAccessor(Context context) {
    super(context);
    this.deletedQueryHelper = new RepoUtils.QueryHelper(context, getDeletedUri(), LOG_TAG());
  }

  abstract protected Uri getDeletedUri();

  public void wipe() {
    super.wipe();
    try {
      purgeDeleted();
    } catch (NullCursorException e) {
      // We tried -- ignore for now.
    }
  }

  protected void addToDeletedTable(String guid, long timestampInMillis) {
    ContentValues cv = new ContentValues();

    // cv.put(BrowserContract.DeletedColumns.ID,           rec.androidID);
    cv.put(BrowserContract.DeletedColumns.GUID, guid);
    cv.put(BrowserContract.DeletedColumns.TIME_DELETED, timestampInMillis);

    context.getContentResolver().insert(getDeletedUri(), cv);
  }

  protected void delete(String guid) {
    addToDeletedTable(guid, System.currentTimeMillis());
    super.delete(guid);
  }

  @Override
  public void purgeDeleted() throws NullCursorException {
    Uri deletedUri = getDeletedUri();
    Logger.info(LOG_TAG(), "purgeDeleted: " + deletedUri);
    context.getContentResolver().delete(deletedUri, null, null);
  }

  /**
   * Dump both the records and the deleted records.
   */
  public void dumpDB() {
    Cursor cur = null;
    try {
      cur = queryHelper.safeQuery(".dumpDB", null, null, null, null);
      RepoUtils.dumpCursor(cur, 18, "records");
      cur.close();
      cur = null;
      cur = deletedQueryHelper.safeQuery(".dumpDB", null, null, null, null);
      RepoUtils.dumpCursor(cur, 18, "deleted records");
    } catch (NullCursorException e) {
    } finally {
      if (cur != null) {
        cur.close();
      }
    }
  }
}
