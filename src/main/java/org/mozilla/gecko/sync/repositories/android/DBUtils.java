/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Android Sync Client.
 *
 * The Initial Developer of the Original Code is
 * the Mozilla Foundation.
 * Portions created by the Initial Developer are Copyright (C) 2011
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Jason Voll <jvoll@mozilla.com>
 *   Richard Newman <rnewman@mozilla.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either the GNU General Public License Version 2 or later (the "GPL"), or
 * the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the MPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the MPL, the GPL or the LGPL.
 *
 * ***** END LICENSE BLOCK ***** */

package org.mozilla.gecko.sync.repositories.android;

import java.util.HashMap;

import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.mozilla.gecko.R;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.HistoryRecord;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class DBUtils {

  private static final String LOG_TAG = "DBUtils";
  public static String[] SPECIAL_GUIDS = new String[] {
    "menu",
    "places",
    //"tags",
    "toolbar",
    "unfiled",
    //"desktop",
    "mobile"
  };
  public static HashMap<String, String> SPECIAL_GUIDS_MAP;

  // TODO there has got to be a better way of solving this problem,
  // come back to this when brain is less fried!
  public static void initialize(Context context) {
    if (SPECIAL_GUIDS_MAP == null) {
      SPECIAL_GUIDS_MAP = new HashMap<String, String>();
      SPECIAL_GUIDS_MAP.put("menu", context.getString(R.string.menu));
      SPECIAL_GUIDS_MAP.put("places", context.getString(R.string.places));
      //SPECIAL_GUIDS_MAP.put("tags", context.getString(R.string.tags));
      SPECIAL_GUIDS_MAP.put("toolbar", context.getString(R.string.toolbar));
      SPECIAL_GUIDS_MAP.put("unfiled", context.getString(R.string.unfiled));
      //SPECIAL_GUIDS_MAP.put("desktop", context.getString(R.string.desktop));
      SPECIAL_GUIDS_MAP.put("mobile", context.getString(R.string.mobile));
    }
  }

  public static String getStringFromCursor(Cursor cur, String colId) {
    return cur.getString(cur.getColumnIndex(colId));
  }

  public static long getLongFromCursor(Cursor cur, String colId) {
    return cur.getLong(cur.getColumnIndex(colId));
  }

  private static JSONArray getJSONArrayFromCursor(Cursor cur, String colId) {
    try {
      return (JSONArray) new JSONParser().parse(getStringFromCursor(cur, colId));
    } catch (ParseException e) {
      Log.e(LOG_TAG, "JSON parsing error for " + colId, e);
      return null;
    }
  }

  // Returns android id from the URI that we get after inserting a
  // bookmark into the local Android store.
  public static long getAndroidIdFromUri(Uri uri) {
    String path = uri.getPath();
    int lastSlash = path.lastIndexOf('/');
    return Long.parseLong(path.substring(lastSlash + 1));
  }

  //Create a BookmarkRecord object from a cursor on a row with a Moz Bookmark in it
  public static BookmarkRecord bookmarkFromMirrorCursor(Cursor cur, String parentId, String parentName) {

    // TODO NOTE NOTE NOTE carrying on our screwy qualify/not qualify problem,
    // the hard coded column names below are because these must be unqualified!
    //String guid = getStringFromCursor(cur, BrowserContract.Bookmarks.GUID);
    String guid = getStringFromCursor(cur, "guid");
    String collection = "bookmarks";
    long lastModified = getLongFromCursor(cur, "modified");

    BookmarkRecord rec = new BookmarkRecord(guid, collection, lastModified);

    rec.title = getStringFromCursor(cur, BrowserContract.Bookmarks.TITLE);
    rec.bookmarkURI = getStringFromCursor(cur, BrowserContract.Bookmarks.URL);
    //rec.description = getStringFromCursor(cur, BrowserContract.COL_DESCRIP);
    //rec.tags = getJSONArrayFromCursor(curMoz, AndroidBrowserBookmarksDatabaseHelper.COL_TAGS);
    //rec.keyword = getStringFromCursor(cur, AndroidBrowserBookmarksDatabaseHelper.COL_KEYWORD);
    rec.type = cur.getInt(cur.getColumnIndex(BrowserContract.Bookmarks.IS_FOLDER)) == 0 ?
      AndroidBrowserBookmarksDataAccessor.TYPE_BOOKMARK : AndroidBrowserBookmarksDataAccessor.TYPE_FOLDER;

    rec.androidID = getLongFromCursor(cur, BrowserContract.Bookmarks._ID);
    // TODO implement crazy position resolution stuff
    //rec.pos = getStringFromCursor(cur, AndroidBrowserBookmarksDatabaseHelper.COL_POS);

    // Need to restore the parentId since it isn't stored in content provider
    rec.parentID = parentId;
    // Set parent name
    // Always set the parent name for special folders back to default so stuff doesn't go crazy
    if (SPECIAL_GUIDS_MAP.containsKey(rec.parentID)) {
      rec.parentName = SPECIAL_GUIDS_MAP.get(rec.parentID);
    } else {
      rec.parentName = parentName;
    }
    return rec;
  }

  //Create a HistoryRecord object from a cursor on a row with a Moz History record in it
  public static HistoryRecord historyFromMirrorCursor(Cursor cur) {

    String guid = getStringFromCursor(cur, BrowserContract.History.GUID);
    String collection = "history";
    long lastModified = getLongFromCursor(cur,BrowserContract.History.DATE_MODIFIED);

    HistoryRecord rec = new HistoryRecord(guid, collection, lastModified);

    rec.title = getStringFromCursor(cur, BrowserContract.History.TITLE);
    rec.histURI = getStringFromCursor(cur, BrowserContract.History.URL);
    rec.androidID = getLongFromCursor(cur, BrowserContract.History._ID);
    // TODO currently not compatible with our notion of visits
    //rec.visits = getStringFromCursor(cur, AndroidBrowserHistoryDataAccessor.COL_VISITS);
    rec.dateVisited = getLongFromCursor(cur, BrowserContract.History.DATE_LAST_VISITED);

    return rec;
  }

}
