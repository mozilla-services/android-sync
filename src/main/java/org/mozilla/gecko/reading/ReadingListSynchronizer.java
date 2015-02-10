/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.reading;

import org.mozilla.gecko.background.common.PrefsBranch;
import org.mozilla.gecko.db.BrowserContract;

import android.content.ContentProviderClient;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

/**
 * This class implements the multi-phase synchronizing approach described
 * at <https://github.com/mozilla-services/readinglist/wiki/Client-phases>.
 */
public class ReadingListSynchronizer {
  private final PrefsBranch prefs;
  private final ContentProviderClient client;

  public ReadingListSynchronizer(final PrefsBranch prefs, final ContentProviderClient client) {
    this.prefs = prefs;
    this.client = client;
  }

  // N.B., status changes for items that haven't been uploaded yet are dealt with in
  // uploadNewItems.
  public void uploadStatusChanges(final ReadingListSynchronizerDelegate delegate) {
    final Uri url = BrowserContract.READING_LIST_AUTHORITY_URI;
    final String[] projection = new String[] {};        // TODO
    final String selection = "";             // TODO: status-only change.
    final String[] selectionArgs = null;
    final String sortOrder = null;
    try {
      final Cursor cursor = client.query(url, projection, selection, selectionArgs, sortOrder);

      if (cursor == null) {
        delegate.onUnableToSync(new RuntimeException("Malformed SQL."));
        return;
      }

      try {
        int counter = 0;
        while (cursor.moveToNext()) {
          // TODO: process record.
          counter++;
        }
        delegate.onStatusUploadComplete(counter);
      } finally {
        cursor.close();
      }
    } catch (RemoteException e) {
      delegate.onUnableToSync(e);
    }
  }

  public void uploadNewItems(final ReadingListSynchronizerDelegate delegate) {
    // N.B., query for items that have no GUID, regardless of status.
  }

  public void uploadOutgoing(final ReadingListSynchronizerDelegate delegate) {
  }

  public void sync(final ReadingListSynchronizerDelegate delegate) {
  }
}
