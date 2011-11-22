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

import java.util.ArrayList;
import java.util.Iterator;

import org.mozilla.android.sync.repositories.domain.BookmarkRecord;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Browser;

public class LocalBookmarkSynchronizer {
  /*
   * This class carries out a local sync which is used to
   * keep Mozilla's Bookmark sync database in sync with
   * Android's stock bookmark database.
   */

  // TODO eventually make this a thread or integrate it
  // into some sort of service.

  public static final String MOBILE_PARENT_ID = "mobile";
  public static final String MOBILE_PARENT_NAME = "mobile";
  public static final String BOOKMARK_TYPE = "bookmark";      
  
  private Context context;
  private BookmarksDatabaseHelper dbHelper;
  private ArrayList<Long> visitedDroidIds = new ArrayList<Long>();

  public LocalBookmarkSynchronizer(Context context) {
    this.context = context;
    this.dbHelper = new BookmarksDatabaseHelper(context);
  }

  /*
   * This sync must happen before performing each sync from device
   * to sync server. This pulls all changes from the stock bookmarks
   * db to the local Moz bookmark db.
   */
  public void syncStockToMoz() {

    // Get all bookmarks from Moz table (which will reflect last
    // known state of stock bookmarks - i.e. a snapshot)
    Cursor curMoz = dbHelper.fetchAllBookmarksOrderByAndroidId();
    curMoz.moveToFirst();

    while (curMoz.isAfterLast() == false) {
      
      // Find bookmark in android store
      long androidId = DBUtils.getLongFromCursor(curMoz, BookmarksDatabaseHelper.COL_ANDROID_ID);
      String where = Browser.BookmarkColumns._ID + "=" + androidId;
      Cursor curDroid = context.getContentResolver().query(Browser.BOOKMARKS_URI, null, where, null, null);
      curDroid.moveToFirst();
      
      String guid = DBUtils.getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_GUID); 
      
      // Check if bookmark has been deleted or modified
      if (curDroid.isAfterLast()) {
        dbHelper.markDeleted(guid);
      } else if (!bookmarksSame(curMoz, curDroid)) {
        dbHelper.updateTitleUri(guid, 
            DBUtils.getStringFromCursor(curDroid, Browser.BookmarkColumns.TITLE),
            DBUtils.getStringFromCursor(curDroid, Browser.BookmarkColumns.URL));
      }
      
      visitedDroidIds.add(androidId);
      curDroid.close();
    }
    
    curMoz.close();
    
    // Find any bookmarks in the local store that we didn't visit
    // and add them to our local snapshot
    String[] columns = new String[] {
        Browser.BookmarkColumns._ID,
        Browser.BookmarkColumns.URL,
        Browser.BookmarkColumns.TITLE
    };
    
    Iterator<Long> it = visitedDroidIds.listIterator();
    String where = Browser.BookmarkColumns._ID + " NOT IN (";
    while (it.hasNext()) {
      long id = it.next();
      where = where + id + ", ";      
    }
    where = where.substring(0, where.length() -2) + ")";
    Cursor curNew = context.getContentResolver().query(Browser.BOOKMARKS_URI, columns, where, null, null);
    curNew.moveToFirst();
    
    while (!curNew.isAfterLast()) {
      dbHelper.insertBookmark(
          createBookmark(curNew));     
    }
    
    curNew.close();
    
  }
  
  // Apply changes from local snapshot to stock bookmarks db
  // Must provide a list of guids modified in the last sync
  public void syncMozToStock(String[] guids) {
    
    // Fetch records for guids
    Cursor curMoz = dbHelper.fetch(guids);
    curMoz.moveToFirst();
    
    while (!curMoz.isAfterLast()) {
      
      long androidId = DBUtils.getLongFromCursor(curMoz, BookmarksDatabaseHelper.COL_ANDROID_ID);
      
      // Handle deletions
      boolean deleted = DBUtils.getLongFromCursor(curMoz, BookmarksDatabaseHelper.COL_DELETED) == 1 ? true:false;
      if (deleted) {
        context.getContentResolver().delete(Browser.BOOKMARKS_URI, Browser.BookmarkColumns._ID + "=" + androidId, null);
      } else {
      
        // Check if a record with the given android Id already exists
        Cursor curDroid = context.getContentResolver().query(Browser.BOOKMARKS_URI, null, Browser.BookmarkColumns._ID + "=" + androidId, null, null);
        curDroid.moveToFirst();
        
        String title = DBUtils.getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_TITLE);
        String uri = DBUtils.getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_BMK_URI);
        ContentValues cv = getContentValuesStock(title, uri);
        
        if (curDroid.isAfterLast()) {
          // Handle insertions
          
          Uri newRecord = context.getContentResolver().insert(Browser.BOOKMARKS_URI , cv);
          // TODO figure out how to get id from Uri and write it out to the moz snapshotted record
          
        } else {
          // Handle updates
          
          int rows = context.getContentResolver().update(
              Browser.BOOKMARKS_URI, cv, Browser.BookmarkColumns._ID + "=" + androidId, null);
          // TODO check that number of rows modified is 1, if not, scream bloody murder!
        }
        
        curDroid.close();
      }
      
      curMoz.moveToNext();
    }
    
    curMoz.close();
  }
  
  // Check if two bookmarks are the same
  private boolean bookmarksSame(Cursor curMoz, Cursor curDroid) {
    
    String mozTitle = DBUtils.getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_TITLE);
    String droidTitle = DBUtils.getStringFromCursor(curDroid, Browser.BookmarkColumns.TITLE);
    if (!mozTitle.equals(droidTitle)) return false;
    
    String mozUri = DBUtils.getStringFromCursor(curMoz, BookmarksDatabaseHelper.COL_BMK_URI);
    String droidUri = DBUtils.getStringFromCursor(curDroid, Browser.BookmarkColumns.URL);
    if (!mozUri.equals(droidUri)) return false;
    
    return true;
  }
  
  // Create new moz bookmark from droid cursor
  // containing title, url, id
  private BookmarkRecord createBookmark(Cursor curDroid) {
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
    return rec;    
  }
  
  // Create content values object for insertion into android db
  private ContentValues getContentValuesStock(String title, String uri) {
    ContentValues cv = new ContentValues();
    cv.put(Browser.BookmarkColumns.BOOKMARK, 1);
    cv.put(Browser.BookmarkColumns.TITLE, title);
    cv.put(Browser.BookmarkColumns.URL, uri);
    // Making assumption that android's db has defaults for the other fields
    return cv;
  }
  
  // Check if two bookmarks are the same
  private boolean bookmarksSame(Cursor curMoz, Cursor curDroid) {
    
    String mozTitle = getStringFromColumn(curMoz, BookmarksDatabaseHelper.COL_TITLE);
    String droidTitle = getStringFromColumn(curDroid, Browser.BookmarkColumns.TITLE);
    if (!mozTitle.equals(droidTitle)) return false;
    
    String mozUri = getStringFromColumn(curMoz, BookmarksDatabaseHelper.COL_BMK_URI);
    String droidUri = getStringFromColumn(curDroid, Browser.BookmarkColumns.URL);
    if (!mozUri.equals(droidUri)) return false;
    
    return true;
  }
  
  private String getStringFromColumn(Cursor cur, String colId) {
    return cur.getString(cur.getColumnIndex(colId));
  }


  

}
