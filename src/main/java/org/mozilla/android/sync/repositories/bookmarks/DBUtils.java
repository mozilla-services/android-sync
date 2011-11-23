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

import org.mozilla.android.sync.repositories.domain.BookmarkRecord;

import android.database.Cursor;
import android.net.Uri;
import android.provider.Browser;

public class DBUtils {
  
  public static final String MOBILE_PARENT_ID = "mobile";
  public static final String MOBILE_PARENT_NAME = "mobile";
  public static final String BOOKMARK_TYPE = "bookmark";      
  
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
  
  //Create a BookmarkRecord object from a cursor on a row with a Moz Bookmark in it
  public static BookmarkRecord bookmarkFromMozCursor(Cursor curMoz) {

    String guid = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_GUID);
    String collection = "bookmarks";
    long lastModified = getLongFromCursor(curMoz, BookmarksDatabaseHelper.COL_LAST_MOD);

    BookmarkRecord rec = new BookmarkRecord(guid, collection, lastModified);

    rec.androidID = getLongFromCursor(curMoz, BookmarksDatabaseHelper.COL_ANDROID_ID);
    rec.title = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_TITLE);
    rec.bookmarkURI = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_BMK_URI);
    rec.description = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_DESCRIP);
    rec.loadInSidebar = curMoz.getInt(curMoz.getColumnIndex(BookmarksDatabaseHelper.COL_LOAD_IN_SIDEBAR)) == 1 ? true: false;
    rec.tags = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_TAGS);
    rec.keyword = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_KEYWORD);
    rec.parentID = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_PARENT_ID);
    rec.parentName = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_PARENT_NAME);
    rec.type = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_TYPE);
    rec.generatorURI = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_GENERATOR_URI);
    rec.staticTitle = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_STATIC_TITLE);
    rec.folderName = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_FOLDER_NAME);
    rec.queryID = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_QUERY_ID);
    rec.siteURI = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_SITE_URI);
    rec.feedURI = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_FEED_URI);
    rec.pos = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_POS);
    rec.children = getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_CHILDREN);
    rec.deleted = curMoz.getInt(curMoz.getColumnIndex(BookmarksDatabaseHelper.COL_DELETED)) == 1 ? true: false;
    return rec;
  }
 
  // Create a BookmarkRecord object from a cursor on a row with a Android stock Bookmark in it
  public static BookmarkRecord bookmarkFromAndroidCursor(Cursor curDroid) {
    String title = DBUtils.getStringFromCursor(curDroid, Browser.BookmarkColumns.TITLE);
    String uri = DBUtils.getStringFromCursor(curDroid, Browser.BookmarkColumns.URL);
    long droidId = DBUtils.getLongFromCursor(curDroid, Browser.BookmarkColumns._ID);
   
    BookmarkRecord rec = new BookmarkRecord();
    rec.androidID = droidId;
    rec.loadInSidebar = false;
    rec.title = title;
    rec.bookmarkURI = uri;
    rec.description = "";
    rec.tags = "";
    rec.keyword = "";
    rec.parentID = MOBILE_PARENT_ID;
    rec.parentName = MOBILE_PARENT_NAME;
    rec.type = BOOKMARK_TYPE;
    rec.generatorURI = "";
    rec.staticTitle = "";
    rec.folderName = "";
    rec.queryID = "";
    rec.siteURI = "";
    rec.feedURI = "";
    rec.pos = "";
    rec.children = "";
    rec.deleted = false;
    return rec;    
 }

}