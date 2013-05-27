/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.background.db;

import java.io.IOException;

import org.mozilla.gecko.db.BrowserContract;
import org.mozilla.gecko.db.BrowserContract.Bookmarks;

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;

public class BookmarkExporter {
  public static final String LOG_TAG = BookmarkExporter.class.getSimpleName();

  protected final Context context;

  public BookmarkExporter(Context context) {
    this.context = context;
  }

  public interface CB {
    public void open(long type, long id, Cursor cursor) throws IOException;
    public void close(long type, long id, Cursor cursor) throws IOException;
    public void onError(Exception e);
  }

  public void export(CB cb) {
    ContentProviderClient client = context.getContentResolver().acquireContentProviderClient(Bookmarks.CONTENT_URI);
    if (client == null) {
      cb.onError(new Exception("Got null client."));
      return;
    }

    try {
      export(cb, client, Bookmarks.FIXED_ROOT_ID);
    } catch (Exception e) {
      cb.onError(e);
    }
  }

  protected void export(CB cb, ContentProviderClient client, long parentId) throws Exception {
    Cursor cursor = client.query(BrowserContract.Bookmarks.CONTENT_URI, null,
        Bookmarks.PARENT + " = ? AND " + Bookmarks._ID + " != ?",
        new String[] { Long.toString(parentId, 10), Long.toString(parentId, 10) },
        Bookmarks.POSITION + " ASC");
    try {
      int typeIndex = cursor.getColumnIndex(Bookmarks.TYPE);
      int idIndex = cursor.getColumnIndex(Bookmarks._ID);

      cursor.moveToFirst();
      while (!cursor.isAfterLast()) {
        long type = cursor.getLong(typeIndex);
        long id = cursor.getLong(idIndex);

//        if (id == Bookmarks.FIXED_ROOT_ID) {
//          type = Bookmarks.TYPE_FOLDER;
//        }

        cb.open(type, id, cursor);

        if (type == Bookmarks.TYPE_FOLDER) {
//          if (id != Bookmarks.FIXED_ROOT_ID) {
            export(cb, client, id);
//          }
        }

        cb.close(type, id, cursor);

        cursor.moveToNext();
      }
    } finally {
      cursor.close();
    }
  }
}
