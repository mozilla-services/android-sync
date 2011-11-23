package org.mozilla.android.sync.repositories.bookmarks;

import android.database.Cursor;
import android.net.Uri;

public class DBUtils {
  
  public static String getStringFromCursor(Cursor cur, String colId) {
    return cur.getString(cur.getColumnIndex(colId));
  }
  
  public static long getLongFromCursor(Cursor cur, String colId) {
    return cur.getLong(cur.getColumnIndex(colId));
  }
  
  // Returns android id from the uri that we get after inserting a
  // bookmark into the local android store
  public static long getAndroidIdFromUri(Uri uri) {
    String path = uri.getPath();
    int lastSlash = path.lastIndexOf('/');
    return Long.parseLong(path.substring(lastSlash + 1));
  }

}
