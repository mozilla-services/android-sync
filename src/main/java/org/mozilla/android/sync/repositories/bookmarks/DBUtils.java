package org.mozilla.android.sync.repositories.bookmarks;

import android.database.Cursor;

public class DBUtils {
  
  public static String getStringFromCursor(Cursor cur, String colId) {
    return cur.getString(cur.getColumnIndex(colId));
  }
  
  public static long getLongFromCursor(Cursor cur, String colId) {
    return cur.getLong(cur.getColumnIndex(colId));
  }

}
