/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.gecko.sync.repositories.android;

import java.util.HashMap;

import org.json.simple.JSONArray;
import org.mozilla.gecko.sync.repositories.NullCursorException;
import org.mozilla.gecko.sync.repositories.domain.BookmarkRecord;
import org.mozilla.gecko.sync.repositories.domain.Record;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

public class AndroidBrowserBookmarksDataAccessor extends AndroidBrowserRepositoryDataAccessor {

  private static final String LOG_TAG = "BookmarksDataAccessor";

  /*
   * Fragments of SQL to make our lives easier.
   */
  private static final String BOOKMARK_IS_FOLDER = BrowserContract.Bookmarks.IS_FOLDER + " = 1";
  private static final String GUID_NOT_TAGS_OR_PLACES = BrowserContract.SyncColumns.GUID + " NOT IN ('" +
                     BrowserContract.Bookmarks.TAGS_FOLDER_GUID + "', '" +
                     BrowserContract.Bookmarks.PLACES_FOLDER_GUID + "')";

  public static final String TYPE_FOLDER = "folder";
  public static final String TYPE_BOOKMARK = "bookmark";

  private final RepoUtils.QueryHelper queryHelper;

  public AndroidBrowserBookmarksDataAccessor(Context context) {
    super(context);
    this.queryHelper = new RepoUtils.QueryHelper(context, getUri(), LOG_TAG);
  }

  @Override
  protected Uri getUri() {
    return BrowserContract.Bookmarks.CONTENT_URI;
  }

  protected Cursor getGuidsIDsForFolders() throws NullCursorException {
    // Exclude "places" and "tags", in case they've ended up in the DB.
    String where = BOOKMARK_IS_FOLDER + " AND " + GUID_NOT_TAGS_OR_PLACES;
    return queryHelper.safeQuery(".getGuidsIDsForFolders", null, where, null, null);
  }

  protected void updateParentAndPosition(String guid, long newParentId, long position) {
    ContentValues cv = new ContentValues();
    cv.put(BrowserContract.Bookmarks.PARENT, newParentId);
    cv.put(BrowserContract.Bookmarks.POSITION, position);
    updateByGuid(guid, cv);
  } 
  
  /*
   * Verify that all special GUIDs are present and that they aren't marked as deleted.
   * Insert them if they aren't there.
   */
  public void checkAndBuildSpecialGuids() throws NullCursorException {
    Cursor cur = fetch(RepoUtils.SPECIAL_GUIDS);
    long mobileRoot  = 0;
    long desktopRoot = 0;

    // Map from GUID to whether deleted. Non-presence implies just that.
    HashMap<String, Boolean> statuses = new HashMap<String, Boolean>(RepoUtils.SPECIAL_GUIDS.length);
    try {
      if (cur.moveToFirst()) {
        while (!cur.isAfterLast()) {
          String guid = RepoUtils.getStringFromCursor(cur, BrowserContract.SyncColumns.GUID);
          if (guid.equals("mobile")) {
            mobileRoot = RepoUtils.getLongFromCursor(cur, BrowserContract.CommonColumns._ID);
          }
          if (guid.equals("desktop")) {
            desktopRoot = RepoUtils.getLongFromCursor(cur, BrowserContract.CommonColumns._ID);
          }
          // Make sure none of these folders are marked as deleted.
          boolean deleted = RepoUtils.getLongFromCursor(cur, BrowserContract.SyncColumns.IS_DELETED) == 1;
          statuses.put(guid, deleted);
          cur.moveToNext();
        }
      }
    } finally {
      cur.close();
    }

    // Insert or undelete them if missing.
    for (String guid : RepoUtils.SPECIAL_GUIDS) {
      if (statuses.containsKey(guid)) {
        if (statuses.get(guid)) {
          // Undelete.
          Log.i(LOG_TAG, "Undeleting special GUID " + guid);
          ContentValues cv = new ContentValues();
          cv.put(BrowserContract.SyncColumns.IS_DELETED, 0);
          updateByGuid(guid, cv);
        }
      } else {
        // Insert.
        if (guid.equals("mobile")) {
          Log.i(LOG_TAG, "No mobile folder. Inserting one.");
          mobileRoot = insertSpecialFolder("mobile", 0);
        } else if (guid.equals("places")) {
          desktopRoot = insertSpecialFolder("places", mobileRoot);
        } else {
          // unfiled, menu, toolbar.
          insertSpecialFolder(guid, desktopRoot);
        }
      }
    }
  }

  private long insertSpecialFolder(String guid, long parentId) {
    BookmarkRecord record = new BookmarkRecord(guid);
    record.title = RepoUtils.SPECIAL_GUIDS_MAP.get(guid);
    record.type = "folder";
    record.androidParentID = parentId;
    return(RepoUtils.getAndroidIdFromUri(insert(record)));
  }

  @Override
  protected ContentValues getContentValues(Record record) {
    ContentValues cv = new ContentValues();
    BookmarkRecord rec = (BookmarkRecord) record;
    cv.put(BrowserContract.SyncColumns.GUID,      rec.guid);
    cv.put(BrowserContract.Bookmarks.TITLE,       rec.title);
    cv.put(BrowserContract.Bookmarks.URL,         rec.bookmarkURI);
    cv.put(BrowserContract.Bookmarks.DESCRIPTION, rec.description);
    if (rec.tags == null) {
      rec.tags = new JSONArray();
    }
    cv.put(BrowserContract.Bookmarks.TAGS,        rec.tags.toJSONString());
    cv.put(BrowserContract.Bookmarks.KEYWORD,     rec.keyword);
    cv.put(BrowserContract.Bookmarks.PARENT,      rec.androidParentID);
    cv.put(BrowserContract.Bookmarks.POSITION,    rec.androidPosition);

    // Only bookmark and folder types should make it this far.
    // Other types should be filtered out and dropped.
    cv.put(BrowserContract.Bookmarks.IS_FOLDER,   rec.type.equalsIgnoreCase(TYPE_FOLDER) ? 1 : 0);

    cv.put("modified", rec.lastModified);
    return cv;
  }
  
  // Returns a cursor with any records that list the given androidID as a parent
  public Cursor getChildren(long androidID) throws NullCursorException {
    String where = BrowserContract.Bookmarks.PARENT + " = ?";
    String[] args = new String[] { String.valueOf(androidID) };
    return queryHelper.safeQuery(".getChildren", getAllColumns(), where, args, null);
  }
  
  @Override
  protected String[] getAllColumns() {
    return BrowserContract.Bookmarks.BookmarkColumns;
  }
}
