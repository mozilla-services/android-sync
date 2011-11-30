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
 * Jason Voll <jvoll@mozilla.com>
 * Richard Newman <rnewman@mozilla.com>
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

package org.mozilla.android.sync.repositories.bookmarks;

import org.mozilla.android.sync.repositories.RepositoryDatabaseHelper;
import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.repositories.domain.Record;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

public class BookmarksDatabaseHelper extends RepositoryDatabaseHelper {

  private static String[] BOOKMARKS_COLUMNS;

  public BookmarksDatabaseHelper(Context context) {
    super(context);
    BOOKMARKS_COLUMNS = new String[] {
        COL_ID, COL_GUID, COL_ANDROID_ID, COL_TITLE,
        COL_BMK_URI, COL_DESCRIP, COL_LOAD_IN_SIDEBAR,
        COL_TAGS, COL_KEYWORD, COL_PARENT_ID, COL_PARENT_NAME,
        COL_TYPE, COL_GENERATOR_URI, COL_STATIC_TITLE,
        COL_FOLDER_NAME, COL_QUERY_ID, COL_SITE_URI,
        COL_FEED_URI, COL_POS, COL_CHILDREN, COL_LAST_MOD,
        COL_DELETED};
  }

  // inserts and return the row id for the bookmark
  public long insert(Record record) {
    SQLiteDatabase db = this.getCachedWritableDatabase();
    ContentValues cv = getContentValues((BookmarkRecord) record);
    long rowId = db.insert(TBL_BOOKMARKS, null, cv);

    Log.i("DBLocal", "Inserted bookmark into row: " + rowId);

    return rowId;
  }

  // returns all bookmark records
  public Cursor fetchAllOrderByAndroidId() {
    SQLiteDatabase db = this.getCachedReadableDatabase();
    Cursor cur = db.query(TBL_BOOKMARKS, BOOKMARKS_COLUMNS, null, null, null, null, COL_ANDROID_ID);
    return cur;
  }

  // get all guids modified since timestamp
  public Cursor getGUIDSSince(long timestamp) {
    SQLiteDatabase db = this.getCachedReadableDatabase();
    Cursor c = db.query(TBL_BOOKMARKS, new String[] {COL_GUID}, COL_LAST_MOD + " >= " +
        Long.toString(timestamp), null, null, null, null);
    return c;
  }

  // get records modified since timestamp
  public Cursor fetchSince(long timestamp) {
    SQLiteDatabase db = this.getCachedReadableDatabase();
    Cursor c = db.query(TBL_BOOKMARKS, BOOKMARKS_COLUMNS, COL_LAST_MOD + " >= " +
        Long.toString(timestamp), null, null, null, null);
    return c;
  }
  
  // get all records requested
  public Cursor fetch(String guids[]) {
    SQLiteDatabase db = this.getCachedReadableDatabase();
    String where = COL_GUID + " in (";
    for (String guid : guids) {
      where = where + "'" + guid + "', ";
    }
    where = (where.substring(0, where.length() -2) + ")");
    String queryString = SQLiteQueryBuilder.buildQueryString(false, TBL_BOOKMARKS, BOOKMARKS_COLUMNS, where, null, null, null, null);
    return db.rawQuery(queryString, null);
  }
  
  // update bookmark title + uri for given guid
  public void updateTitleUri(String guid, String title, String uri) {
    updateByGuid(guid, getTitleUriCV(title, uri));
  }
  
  // update android id
  public void updateAndroidId(String guid, long androidId) {
    ContentValues cv = new ContentValues();
    cv.put(COL_ANDROID_ID, androidId);
    updateByGuid(guid, cv);
  }

  // delete bookmark from database looking up by guid
  public void delete(Record record) {
    SQLiteDatabase db = this.getCachedWritableDatabase();
    String[] where = new String[] { String.valueOf(record.guid) };
    db.delete(TBL_BOOKMARKS, COL_GUID+"=?", where);
  }
  
  @Override
  public void updateByGuid(String guid, ContentValues cv) {
    SQLiteDatabase db = this.getWritableDatabase();
    String[] where = new String[] { String.valueOf(guid) };
    db.update(TBL_BOOKMARKS, cv, COL_GUID + "=?", where);
  }
    
  // Return a ContentValues object containing title + uri
  private ContentValues getTitleUriCV(String title, String uri) {
    ContentValues cv = new ContentValues();
    cv.put(COL_TITLE, title);
    cv.put(COL_BMK_URI, uri);
    return cv;    
  }

  private ContentValues getContentValues(BookmarkRecord record) {
    ContentValues cv = new ContentValues();
    cv.put(COL_GUID,            record.guid);
    cv.put(COL_ANDROID_ID,      record.androidID);
    cv.put(COL_TITLE,           record.title);
    cv.put(COL_BMK_URI,         record.bookmarkURI);
    cv.put(COL_DESCRIP,         record.description);
    cv.put(COL_LOAD_IN_SIDEBAR, record.loadInSidebar ? 1 : 0);
    cv.put(COL_TAGS,            record.tags);
    cv.put(COL_KEYWORD,         record.keyword);
    cv.put(COL_PARENT_ID,       record.parentID);
    cv.put(COL_PARENT_NAME,     record.parentName);
    cv.put(COL_TYPE,            record.type);
    cv.put(COL_GENERATOR_URI,   record.generatorURI);
    cv.put(COL_STATIC_TITLE,    record.staticTitle);
    cv.put(COL_FOLDER_NAME,     record.folderName);
    cv.put(COL_QUERY_ID,        record.queryID);
    cv.put(COL_SITE_URI,        record.siteURI);
    cv.put(COL_FEED_URI,        record.feedURI);
    cv.put(COL_POS,             record.pos);
    cv.put(COL_CHILDREN,        record.children);
    cv.put(COL_LAST_MOD,        record.lastModified);
    cv.put(COL_DELETED,         record.deleted);
    return cv;
  }
}
