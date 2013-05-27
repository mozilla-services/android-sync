package org.mozilla.gecko.background.db.test;

import java.io.IOException;

import org.json.simple.JSONValue;
import org.mozilla.gecko.background.helpers.AndroidSyncTestCase;
import org.mozilla.gecko.db.BrowserContract.Bookmarks;
import org.mozilla.gecko.sync.repositories.android.RepoUtils;

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.os.RemoteException;

public class TestBookmarkExporter extends AndroidSyncTestCase {
  public static final String LOG_TAG = TestBookmarkExporter.class.getSimpleName();

  public long queryCount = 0;

  public void dump(final Context context) throws IOException {
    ContentProviderClient client = context.getContentResolver().acquireContentProviderClient(Bookmarks.CONTENT_URI);
    try {
      Appendable buffer = System.out;
      buffer.append("{\"root\":null");
      dump(client, buffer, 0);
      buffer.append("}");
      System.out.flush();
    } finally {
      if (client != null) {
        client.release();
      }
    }
  }

  public void append(Appendable buffer, String key, Object value) throws IOException {
    if (key.contains("\"")) {
      throw new IllegalArgumentException("key must not contain quotes");
    }
    buffer.append("\"");
    buffer.append(key);
    buffer.append("\":");
    if (value == null) {
      buffer.append("null");
    } else if (value instanceof String) {
      buffer.append("\"");
      buffer.append(JSONValue.escape((String) value));
      buffer.append("\"");
    } else {
      buffer.append(value.toString());
    }
  }

  public void openSelf(Cursor cur, Appendable buffer) throws IOException {
    buffer.append("{");
//    "id": 1,
    append(buffer, "dateAdded", 1000L * RepoUtils.getLongFromCursor(cur, Bookmarks.DATE_CREATED));
    buffer.append(",");
    append(buffer, "lastModified", 1000L * RepoUtils.getLongFromCursor(cur, Bookmarks.DATE_MODIFIED));
    buffer.append(",");
    append(buffer, "title", RepoUtils.getStringFromCursor(cur, Bookmarks.TITLE));
    buffer.append(",");
    append(buffer, "id", RepoUtils.getIntFromCursor(cur, Bookmarks._ID));
    buffer.append(",");
    append(buffer, "parent", RepoUtils.getIntFromCursor(cur, Bookmarks.PARENT));
    buffer.append(",");
    append(buffer, "index", cur.getPosition());

    int type = RepoUtils.getIntFromCursor(cur, Bookmarks.TYPE);
    switch (type) {
    case Bookmarks.TYPE_BOOKMARK:
      buffer.append(",");
      append(buffer, "uri", RepoUtils.getStringFromCursor(cur, Bookmarks.URL));
      buffer.append(",");
      append(buffer, "type", "text/x-moz-place");
      break;
    case Bookmarks.TYPE_FOLDER:
      buffer.append(",");
      append(buffer, "type", "text/x-moz-place-container");
      break;
    }

    // buffer.append(",");
//    "lastModified": 1334874594818404,
//    "root": "placesRoot",
//    "title": "",
//    "type": "text/x-moz-place-container"

  }

  public void closeSelf(Cursor cur, Appendable buffer) throws IOException {
    buffer.append("}");
  }

  public void dump(ContentProviderClient client, Appendable buffer, int parentId) throws IOException {
    if (client == null) {
      throw new IllegalArgumentException("client must not be null");
    }
    Cursor children = null;
    try {
      String[] columns = new String[] {
          Bookmarks.TITLE,
          Bookmarks.URL,
          Bookmarks.TYPE,
          Bookmarks.DATE_CREATED,
          Bookmarks.DATE_MODIFIED,
          Bookmarks._ID,
          Bookmarks.PARENT,
      };

      children = client.query(Bookmarks.CONTENT_URI, columns, Bookmarks.PARENT + " = ? AND " + Bookmarks._ID + " != 0", new String[] { Integer.toString(parentId) }, Bookmarks.PARENT + " ASC");
      queryCount += 1;

      children.moveToFirst();
      if (children.isAfterLast()) {
        return;
      }
      buffer.append(",\"xchildren\":[");
      while (!children.isAfterLast()) {
        openSelf(children, buffer);
        int type = RepoUtils.getIntFromCursor(children, Bookmarks.TYPE);
        if (type == Bookmarks.TYPE_FOLDER) {
          dump(client, buffer, RepoUtils.getIntFromCursor(children, Bookmarks._ID));
        }
        closeSelf(children, buffer);
        children.moveToNext();
        if (!children.isAfterLast()) {
          buffer.append(",");
        }
      }
      buffer.append("]");
    } catch (RemoteException e) {
      throw new RuntimeException(e);
    } finally {
      if (children != null) {
        children.close();
      }
    }
  }

  public void test() throws Exception {
    dump(this.getApplicationContext());
    System.out.println("queryCount: " + queryCount);
  }
}
