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

package org.mozilla.android.sync.repositories.android;

import org.mozilla.android.sync.repositories.domain.BookmarkRecord;
import org.mozilla.android.sync.repositories.domain.Record;

import android.content.ContentValues;
import android.content.Context;

public class AndroidBrowserBookmarksDatabaseHelper extends AndroidBrowserRepositoryDatabaseHelper {

  private static String[] BOOKMARKS_COLUMNS;

  public AndroidBrowserBookmarksDatabaseHelper(Context context) {
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

  @Override
  public String getTable() {
    return TBL_BOOKMARKS;
  }
  
  @Override
  public String[] getAllColumns() {
    return BOOKMARKS_COLUMNS;
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
    
  // Return a ContentValues object containing title + uri
  private ContentValues getTitleUriCV(String title, String uri) {
    ContentValues cv = new ContentValues();
    cv.put(COL_TITLE, title);
    cv.put(COL_BMK_URI, uri);
    return cv;    
  }

  @Override
  protected ContentValues getContentValues(Record record) {
    ContentValues cv = new ContentValues();
    BookmarkRecord rec = (BookmarkRecord) record;
    cv.put(COL_GUID,            rec.guid);
    cv.put(COL_ANDROID_ID,      rec.androidID);
    cv.put(COL_TITLE,           rec.title);
    cv.put(COL_BMK_URI,         rec.bookmarkURI);
    cv.put(COL_DESCRIP,         rec.description);
    cv.put(COL_LOAD_IN_SIDEBAR, rec.loadInSidebar ? 1 : 0);
    cv.put(COL_TAGS,            rec.tags);
    cv.put(COL_KEYWORD,         rec.keyword);
    cv.put(COL_PARENT_ID,       rec.parentID);
    cv.put(COL_PARENT_NAME,     rec.parentName);
    cv.put(COL_TYPE,            rec.type);
    cv.put(COL_GENERATOR_URI,   rec.generatorURI);
    cv.put(COL_STATIC_TITLE,    rec.staticTitle);
    cv.put(COL_FOLDER_NAME,     rec.folderName);
    cv.put(COL_QUERY_ID,        rec.queryID);
    cv.put(COL_SITE_URI,        rec.siteURI);
    cv.put(COL_FEED_URI,        rec.feedURI);
    cv.put(COL_POS,             rec.pos);
    cv.put(COL_CHILDREN,        rec.children);
    cv.put(COL_LAST_MOD,        rec.lastModified);
    cv.put(COL_DELETED,         rec.deleted);
    return cv;
  }
}
